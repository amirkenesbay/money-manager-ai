package ai.moneymanager.web.voice

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.OperationSuggestion
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.AiRateLimitResult
import ai.moneymanager.service.AiRateLimitService
import ai.moneymanager.service.CategoryRuleService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.service.nlp.WAV_MIME_TYPE
import ai.moneymanager.service.nlp.suggestOperation
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.badRequest
import ai.moneymanager.web.time.UserCalendar
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

const val MAX_VOICE_BYTES = 2_500_000
const val MAX_VOICE_OPERATIONS = 10

private const val INVALID_AUDIO_KEY = "${API_ERROR_KEY_PREFIX}invalid_audio"
private const val WAV_HEADER_SIZE = 44
private const val RIFF_MARKER = "RIFF"
private const val WAVE_MARKER = "WAVE"
private const val WAVE_MARKER_OFFSET = 8
private const val LIMIT_DETAIL = "limit"
private const val RESET_DETAIL = "resetInSeconds"
private const val RETRY_DETAIL = "retryAfterSeconds"
private const val SECONDS_IN_HOUR = 3600L

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/voice")
class VoiceController(
    private val groupAccessGuard: GroupAccessGuard,
    private val categoryService: CategoryService,
    private val categoryRuleService: CategoryRuleService,
    private val aiRateLimitService: AiRateLimitService,
    private val aiPromptService: AiPromptService,
    private val commandParserService: CommandParserService,
    private val userCalendar: UserCalendar,
) {
    @PostMapping(consumes = [WAV_MIME_TYPE])
    fun parse(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody audio: ByteArray): VoiceResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        requireWav(audio)
        requireAiQuota(user)
        val categories = categoryService.getCategoriesByGroup(groupId)
        val rules = categoryRuleService.rules(groupId)
        val commands = commandParserService.parseVoiceCommands(audio, aiPromptService.groupContext(group.currency, categories, rules), WAV_MIME_TYPE)
        requireParsed(commands)
        val today = userCalendar.today(user)
        val operations = commands
            .mapNotNull { suggestOperation(it, categories, today, rules)?.toResponse() }
            .take(MAX_VOICE_OPERATIONS)
        return VoiceResponse(operations)
    }

    private fun requireWav(audio: ByteArray) {
        val wav = audio.size in (WAV_HEADER_SIZE + 1)..MAX_VOICE_BYTES &&
            marker(audio, 0) == RIFF_MARKER &&
            marker(audio, WAVE_MARKER_OFFSET) == WAVE_MARKER
        if (!wav) throw badRequest(INVALID_AUDIO_KEY)
    }

    private fun marker(audio: ByteArray, offset: Int): String = String(audio, offset, RIFF_MARKER.length, Charsets.US_ASCII)

    private fun requireAiQuota(user: UserInfo) {
        val result = aiRateLimitService.tryConsume(checkNotNull(user.telegramUserId), user.hasActivePaidSubscription())
        if (result !is AiRateLimitResult.Exceeded) return
        val resetInHours = (result.resetInSeconds / SECONDS_IN_HOUR).coerceAtLeast(1)
        throw ApiException(
            code = ApiErrorCode.AI_LIMIT_REACHED,
            messageArgs = listOf(result.limit, resetInHours),
            details = mapOf(LIMIT_DETAIL to result.limit, RESET_DETAIL to result.resetInSeconds),
        )
    }

    private fun requireParsed(commands: List<BotCommand>) {
        commands.filterIsInstance<BotCommand.RateLimitError>().firstOrNull()?.let { busy ->
            throw ApiException(ApiErrorCode.AI_BUSY, details = busy.retryAfterSeconds?.let { mapOf(RETRY_DETAIL to it) }.orEmpty())
        }
        if (commands.any { it is BotCommand.ServiceError || it is BotCommand.ParseError }) throw ApiException(ApiErrorCode.AI_UNAVAILABLE)
    }

    private fun OperationSuggestion.toResponse(): VoiceOperationResponse? = when (this) {
        is OperationSuggestion.Ready ->
            voiceOperation(type, amount, operationDate, description, category.id, category.name, category.icon)
        is OperationSuggestion.NewCategory ->
            voiceOperation(type, amount, operationDate, description, null, categoryName, categoryIcon, isNewCategory = true)
        is OperationSuggestion.AmbiguousCategory -> voiceOperation(type, amount, operationDate, description)
        is OperationSuggestion.CategoryMissing -> voiceOperation(type, amount, operationDate, description)
        OperationSuggestion.AmountNotPositive -> null
    }

    private fun voiceOperation(
        type: CategoryType,
        amount: Double,
        operationDate: LocalDate,
        description: String?,
        categoryId: ObjectId? = null,
        categoryName: String? = null,
        categoryIcon: String? = null,
        isNewCategory: Boolean = false,
    ) = VoiceOperationResponse(type, BigDecimal.valueOf(amount), categoryId, categoryName, categoryIcon, isNewCategory, operationDate, description)
}
