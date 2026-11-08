package ai.moneymanager.web.ai

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryRule
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.AiRateLimitResult
import ai.moneymanager.service.AiRateLimitService
import ai.moneymanager.service.SharedMembers
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.question.AskService
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.time.UserCalendar
import org.springframework.stereotype.Component

private const val LIMIT_DETAIL = "limit"
private const val RESET_DETAIL = "resetInSeconds"
private const val RETRY_DETAIL = "retryAfterSeconds"
private const val SECONDS_IN_HOUR = 3600L

@Component
class AiRequestGuard(
    private val aiRateLimitService: AiRateLimitService,
    private val aiPromptService: AiPromptService,
    private val askService: AskService,
    private val userInfoService: UserInfoService,
    private val userCalendar: UserCalendar,
) {
    fun requireQuota(user: UserInfo) {
        val result = aiRateLimitService.tryConsume(checkNotNull(user.telegramUserId), user.hasActivePaidSubscription())
        if (result !is AiRateLimitResult.Exceeded) return
        val resetInHours = (result.resetInSeconds / SECONDS_IN_HOUR).coerceAtLeast(1)
        throw ApiException(
            code = ApiErrorCode.AI_LIMIT_REACHED,
            messageArgs = listOf(result.limit, resetInHours),
            details = mapOf(LIMIT_DETAIL to result.limit, RESET_DETAIL to result.resetInSeconds),
        )
    }

    fun tryQuota(user: UserInfo): Boolean =
        aiRateLimitService.tryConsume(checkNotNull(user.telegramUserId), user.hasActivePaidSubscription()) !is AiRateLimitResult.Exceeded

    fun requireParsed(commands: List<BotCommand>) {
        commands.filterIsInstance<BotCommand.RateLimitError>().firstOrNull()?.let { busy ->
            throw ApiException(ApiErrorCode.AI_BUSY, details = busy.retryAfterSeconds?.let { mapOf(RETRY_DETAIL to it) }.orEmpty())
        }
        if (commands.any { it is BotCommand.ServiceError || it is BotCommand.ParseError }) throw ApiException(ApiErrorCode.AI_UNAVAILABLE)
    }

    fun groupContext(user: UserInfo, group: MoneyGroup, categories: List<Category>, rules: List<CategoryRule>): String =
        aiPromptService.groupContext(userCalendar.today(user), group.currency, categories, rules, askService.previousQuestion(user), sharedMembersOf(user, group))

    fun memberNames(group: MoneyGroup): Map<Long, String> =
        if (group.splitsExpenses) userInfoService.displayNames(group.memberIds) else emptyMap()

    private fun sharedMembersOf(user: UserInfo, group: MoneyGroup): SharedMembers? {
        val names = memberNames(group).takeIf { it.isNotEmpty() } ?: return null
        return SharedMembers(names.getValue(checkNotNull(user.telegramUserId)), names.values.toList())
    }
}
