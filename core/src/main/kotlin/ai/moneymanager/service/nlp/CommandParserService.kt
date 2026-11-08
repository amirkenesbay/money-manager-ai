package ai.moneymanager.service.nlp

import ai.moneymanager.metrics.GEMINI_QUOTA_STATUS
import ai.moneymanager.metrics.GeminiCall
import ai.moneymanager.metrics.GeminiMetrics
import ai.moneymanager.domain.text.geminiValue
import ai.moneymanager.config.GeminiProperties
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.arguments.AddExpenseArgs
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.domain.model.nlp.arguments.AddIncomeArgs
import ai.moneymanager.domain.model.nlp.arguments.AddLoanArgs
import ai.moneymanager.domain.model.nlp.arguments.RepayLoanArgs
import ai.moneymanager.domain.model.nlp.arguments.AskFinanceArgs
import ai.moneymanager.service.question.financeQuestionOf
import ai.moneymanager.domain.model.nlp.arguments.ChangeCategoryIconArgs
import ai.moneymanager.domain.model.nlp.arguments.CreateCategoryArgs
import ai.moneymanager.domain.model.nlp.arguments.CreateGroupArgs
import ai.moneymanager.domain.model.nlp.arguments.CreateNotificationArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteCategoryArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteGroupArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteLastOperationArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteNotificationArgs
import ai.moneymanager.domain.model.nlp.arguments.EditLastOperationArgs
import ai.moneymanager.domain.model.nlp.arguments.ListCategoriesArgs
import ai.moneymanager.domain.model.nlp.arguments.RenameCategoryArgs
import ai.moneymanager.domain.model.nlp.arguments.ShowHistoryArgs
import ai.moneymanager.domain.model.nlp.arguments.ShowReportArgs
import ai.moneymanager.domain.model.nlp.arguments.SwitchGroupArgs
import ai.moneymanager.domain.model.nlp.enum.GeminiFunction
import ai.moneymanager.mapper.GeminiArgsMapper
import com.google.genai.Client
import com.google.genai.errors.ApiException
import com.google.genai.errors.ClientException
import com.google.genai.errors.GenAiIOException
import com.google.genai.errors.ServerException
import com.google.genai.types.AutomaticFunctionCallingConfig
import com.google.genai.types.Blob
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.ThinkingConfig
import com.google.genai.types.ThinkingLevel
import com.google.genai.types.Tool
import jakarta.annotation.PostConstruct
import ai.moneymanager.domain.model.nlp.enum.AiSurface
import java.time.LocalDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

const val TELEGRAM_VOICE_MIME_TYPE = "audio/ogg"
const val WAV_MIME_TYPE = "audio/wav"

@Service
class CommandParserService(
    private val geminiProperties: GeminiProperties,
    private val argsMapper: GeminiArgsMapper,
    private val aiPromptService: AiPromptService,
    private val geminiMetrics: GeminiMetrics
) {
    private lateinit var client: Client
    private lateinit var botConfig: GenerateContentConfig
    private lateinit var miniAppConfig: GenerateContentConfig

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        client = Client.builder()
            .apiKey(geminiProperties.apiKey)
            .build()

        val toolDocs = argsMapper.objectMapper.readValue(aiPromptService.toolDocsJson, GeminiToolDocs::class.java)
        val systemContent = Content.fromParts(Part.fromText(aiPromptService.systemPrompt))
        botConfig = configOf(systemContent, toolDocs, AiSurface.BOT)
        miniAppConfig = configOf(systemContent, toolDocs, AiSurface.MINI_APP)
    }

    private fun configOf(systemContent: Content, toolDocs: GeminiToolDocs, surface: AiSurface): GenerateContentConfig {
        val declarations = GeminiFunction.entries.filter { surface.offers(it) }.map { toolDocs.declarationOf(it.method) }
        return GenerateContentConfig.builder()
            .tools(Tool.builder().functionDeclarations(declarations).build())
            .systemInstruction(systemContent)
            .automaticFunctionCalling(AutomaticFunctionCallingConfig.builder().disable(true).build())
            .thinkingConfig(ThinkingConfig.builder().thinkingLevel(ThinkingLevel.Known.MINIMAL).build())
            .build()
    }

    private fun configFor(surface: AiSurface): GenerateContentConfig = when (surface) {
        AiSurface.BOT -> botConfig
        AiSurface.MINI_APP -> miniAppConfig
    }

    fun parseCommand(userMessage: String, categoryContext: String? = null): BotCommand =
        parseCommands(userMessage, categoryContext).first()

    fun parseVoiceCommand(audioBytes: ByteArray, categoryContext: String? = null): BotCommand =
        parseVoiceCommands(audioBytes, categoryContext).first()

    fun parseCommands(userMessage: String, categoryContext: String? = null, surface: AiSurface = AiSurface.BOT): List<BotCommand> =
        generate(GeminiCall.COMMAND, surface, userMessage) { contentWithCategoryContext(categoryContext, Part.fromText(userMessage)) }

    fun parseVoiceCommands(
        audioBytes: ByteArray,
        categoryContext: String? = null,
        mimeType: String = TELEGRAM_VOICE_MIME_TYPE,
        surface: AiSurface = AiSurface.BOT,
    ): List<BotCommand> {
        log.info("Processing voice message: ${audioBytes.size} bytes")
        return generate(GeminiCall.VOICE, surface, VOICE_MESSAGE_PLACEHOLDER) {
            val audioPart = Part.builder().inlineData(Blob.builder().mimeType(mimeType).data(audioBytes).build()).build()
            contentWithCategoryContext(categoryContext, audioPart, Part.fromText(aiPromptService.voiceTranscriptionPrompt))
        }
    }

    private fun generate(call: GeminiCall, surface: AiSurface, originalMessage: String, content: () -> Content): List<BotCommand> {
        val commands = try {
            val response = geminiMetrics.timed(call) { client.models.generateContent(geminiProperties.model, content(), configFor(surface)) }
            processResponse(response, originalMessage)
        } catch (e: Exception) {
            log.error("CommandParser ${call.tag} error: ${e.message}", e)
            listOf(classifyError(e))
        }
        return commands.also { geminiMetrics.recordResults(surface, it) }
    }

    private fun contentWithCategoryContext(categoryContext: String?, vararg parts: Part): Content {
        val allParts = buildList {
            add(Part.fromText(categoryContext?.takeIf { it.isNotBlank() } ?: aiPromptService.currentDatePreamble(LocalDate.now())))
            addAll(parts)
        }
        return Content.builder().parts(allParts).build()
    }

    private fun classifyError(e: Exception): BotCommand {
        return when (e) {
            is ClientException -> {
                if (e.code() == GEMINI_QUOTA_STATUS) {
                    BotCommand.RateLimitError(parseRetryAfterSeconds(e.message()))
                } else {
                    BotCommand.ServiceError
                }
            }
            is ServerException -> BotCommand.ServiceError
            is ApiException -> BotCommand.ServiceError
            is GenAiIOException -> BotCommand.ServiceError
            else -> BotCommand.ParseError(e.message ?: "Unknown error")
        }
    }

    private fun parseRetryAfterSeconds(message: String?): Long? {
        if (message.isNullOrBlank()) return null
        val match = RETRY_AFTER_REGEX.find(message) ?: return null
        return match.groupValues[1].toDoubleOrNull()?.toLong()?.coerceAtLeast(1L)
    }

    private fun processResponse(response: com.google.genai.types.GenerateContentResponse, originalMessage: String): List<BotCommand> {
        val functionCalls = response.functionCalls().orEmpty()

        if (functionCalls.isEmpty()) {
            log.info("🤖 Gemini text response: ${response.text()}")
            return listOf(BotCommand.OutOfContext(originalMessage))
        }

        return functionCalls.map { functionCall ->
            log.info("🤖 Gemini function call: ${functionCall.name()} with args: ${functionCall.args()}")
            parseFunctionCall(functionCall, originalMessage)
        }
    }

    private fun parseFunctionCall(functionCall: FunctionCall, originalMessage: String): BotCommand {
        val args = functionCall.args().orElse(emptyMap())
        val function = GeminiFunction.from(functionCall.name().orElse(null))
            ?: return BotCommand.OutOfContext(originalMessage)

        return try {
            when (function) {
                GeminiFunction.CREATE_GROUP ->
                    BotCommand.CreateGroup(requireText(argsMapper.map<CreateGroupArgs>(args).groupName, FIELD_GROUP_NAME))

                GeminiFunction.DELETE_GROUP ->
                    BotCommand.DeleteGroup(requireText(argsMapper.map<DeleteGroupArgs>(args).groupName, FIELD_GROUP_NAME))

                GeminiFunction.ADD_EXPENSE -> {
                    val dto = argsMapper.map<AddExpenseArgs>(args)
                    val repeat = confirmedRepeat(dto.repeatUnit, dto.repeatInterval, dto.repeatPhrase, originalMessage, listOf(dto.description, dto.category))
                    BotCommand.AddExpense(
                        dto.amount, dto.category, dto.description,
                        dto.suggestedCategoryIcon, dto.operationDate.geminiValue(), repeat.unit, repeat.interval, dto.paidBy, dto.splitWith,
                    )
                }

                GeminiFunction.ADD_INCOME -> {
                    val dto = argsMapper.map<AddIncomeArgs>(args)
                    val repeat = confirmedRepeat(dto.repeatUnit, dto.repeatInterval, dto.repeatPhrase, originalMessage, listOf(dto.description, dto.category))
                    BotCommand.AddIncome(
                        dto.amount, dto.category, dto.description,
                        dto.suggestedCategoryIcon, dto.operationDate.geminiValue(), repeat.unit, repeat.interval,
                    )
                }

                GeminiFunction.OUT_OF_CONTEXT -> BotCommand.OutOfContext(originalMessage)

                GeminiFunction.CREATE_CATEGORY -> {
                    val dto = argsMapper.map<CreateCategoryArgs>(args)
                    BotCommand.CreateCategory(
                        requireText(dto.name, FIELD_CATEGORY_NAME),
                        requireText(dto.type, FIELD_CATEGORY_TYPE),
                        dto.icon.geminiValue()
                    )
                }

                GeminiFunction.DELETE_CATEGORY -> {
                    val dto = argsMapper.map<DeleteCategoryArgs>(args)
                    BotCommand.DeleteCategory(requireText(dto.name, FIELD_CATEGORY_NAME), dto.type.geminiValue())
                }

                GeminiFunction.RENAME_CATEGORY -> {
                    val dto = argsMapper.map<RenameCategoryArgs>(args)
                    BotCommand.RenameCategory(
                        requireText(dto.oldName, FIELD_CATEGORY_NAME),
                        requireText(dto.newName, FIELD_CATEGORY_NAME),
                        dto.type.geminiValue()
                    )
                }

                GeminiFunction.CHANGE_CATEGORY_ICON -> {
                    val dto = argsMapper.map<ChangeCategoryIconArgs>(args)
                    BotCommand.ChangeCategoryIcon(
                        requireText(dto.name, FIELD_CATEGORY_NAME),
                        requireText(dto.newIcon, FIELD_CATEGORY_ICON),
                        dto.type.geminiValue()
                    )
                }

                GeminiFunction.DELETE_ALL_CATEGORIES -> BotCommand.DeleteAllCategories

                GeminiFunction.LIST_CATEGORIES ->
                    BotCommand.ListCategories(argsMapper.map<ListCategoriesArgs>(args).type.geminiValue())

                GeminiFunction.LIST_GROUPS -> BotCommand.ListGroups

                GeminiFunction.SWITCH_GROUP ->
                    BotCommand.SwitchGroup(requireText(argsMapper.map<SwitchGroupArgs>(args).groupName, FIELD_GROUP_NAME))

                GeminiFunction.SHOW_BALANCE -> BotCommand.ShowBalance

                GeminiFunction.SHOW_REPORT -> {
                    val dto = argsMapper.map<ShowReportArgs>(args)
                    BotCommand.ShowReport(dto.month?.toInt(), dto.year?.toInt())
                }

                GeminiFunction.SHOW_HISTORY -> {
                    val dto = argsMapper.map<ShowHistoryArgs>(args)
                    BotCommand.ShowHistory(
                        dto.startDate.geminiValue(),
                        dto.endDate.geminiValue(),
                        dto.type.geminiValue(),
                        dto.categoryFilter.geminiValue()
                    )
                }

                GeminiFunction.LIST_NOTIFICATIONS -> BotCommand.ListNotifications

                GeminiFunction.CREATE_NOTIFICATION -> {
                    val dto = argsMapper.map<CreateNotificationArgs>(args)
                    BotCommand.CreateNotification(
                        requireText(dto.name, FIELD_NOTIFICATION_NAME),
                        dto.hour.toInt(),
                        dto.minute?.toInt() ?: 0
                    )
                }

                GeminiFunction.DELETE_NOTIFICATION ->
                    BotCommand.DeleteNotification(requireText(argsMapper.map<DeleteNotificationArgs>(args).name, FIELD_NOTIFICATION_NAME))

                GeminiFunction.DELETE_LAST_OPERATION ->
                    BotCommand.DeleteLastOperation(argsMapper.map<DeleteLastOperationArgs>(args).type.geminiValue())

                GeminiFunction.EDIT_LAST_OPERATION -> {
                    val dto = argsMapper.map<EditLastOperationArgs>(args)
                    BotCommand.EditLastOperation(
                        dto.type.geminiValue(),
                        dto.newAmount,
                        dto.newCategory.geminiValue(),
                        dto.newOperationDate.geminiValue()
                    )
                }

                GeminiFunction.ASK_FINANCE ->
                    financeQuestionOf(argsMapper.map<AskFinanceArgs>(args))?.let { BotCommand.AskFinance(it) } ?: BotCommand.OutOfContext(originalMessage)

                GeminiFunction.ADD_LOAN ->
                    addLoanOf(argsMapper.map<AddLoanArgs>(args)) ?: BotCommand.OutOfContext(originalMessage)

                GeminiFunction.REPAY_LOAN ->
                    repayLoanOf(argsMapper.map<RepayLoanArgs>(args)) ?: BotCommand.OutOfContext(originalMessage)
            }
        } catch (e: Exception) {
            BotCommand.ParseError("Invalid function args for ${functionCall.name().orElse(UNKNOWN_FUNCTION_NAME)}: ${e.message}")
        }
    }

    private fun confirmedRepeat(unit: String?, interval: Double?, phrase: String?, originalMessage: String, itemNames: List<String?>): RepeatArgs {
        val evidence = phrase.geminiValue() ?: return NO_REPEAT
        val heard = originalMessage == VOICE_MESSAGE_PLACEHOLDER || originalMessage.contains(evidence, ignoreCase = true)
        val namesItem = itemNames.any { it != null && (it.contains(evidence, ignoreCase = true) || evidence.contains(it, ignoreCase = true)) }
        return if (heard && !namesItem) RepeatArgs(unit.geminiValue(), interval) else NO_REPEAT
    }

    private data class RepeatArgs(val unit: String?, val interval: Double?)

    private fun requireText(value: String, field: String): String {
        require(value.isNotBlank()) { "$field is blank" }
        return value.trim()
    }

    companion object {
        private const val VOICE_MESSAGE_PLACEHOLDER = "[voice message]"
        private val NO_REPEAT = RepeatArgs(null, null)
        private val RETRY_AFTER_REGEX = Regex("""Please retry in (\d+(?:\.\d+)?)s""")

        private const val FIELD_GROUP_NAME = "groupName"
        private const val FIELD_CATEGORY_NAME = "category name"
        private const val FIELD_CATEGORY_TYPE = "category type"
        private const val FIELD_CATEGORY_ICON = "category icon"
        private const val FIELD_NOTIFICATION_NAME = "notification name"
        private const val UNKNOWN_FUNCTION_NAME = "unknown"
    }
}
