package ai.moneymanager.service.nlp

import ai.moneymanager.chat.config.GeminiProperties
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.BotFunctions
import ai.moneymanager.domain.model.nlp.arguments.AddExpenseArgs
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.domain.model.nlp.arguments.AddIncomeArgs
import ai.moneymanager.domain.model.nlp.arguments.ChangeCategoryIconArgs
import ai.moneymanager.domain.model.nlp.arguments.CreateCategoryArgs
import ai.moneymanager.domain.model.nlp.arguments.CreateGroupArgs
import ai.moneymanager.domain.model.nlp.arguments.CreateNotificationArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteCategoryArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteGroupArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteNotificationArgs
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
import com.google.genai.types.Tool
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandParserService(
    private val geminiProperties: GeminiProperties,
    private val argsMapper: GeminiArgsMapper,
    private val aiPromptService: AiPromptService
) {
    private lateinit var client: Client
    private lateinit var config: GenerateContentConfig

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        client = Client.builder()
            .apiKey(geminiProperties.apiKey)
            .build()

        // Получаем методы из BotFunctions для function calling
        val createGroupMethod = botFunctionMethod(GeminiFunction.CREATE_GROUP, String::class.java)
        val deleteGroupMethod = botFunctionMethod(GeminiFunction.DELETE_GROUP, String::class.java)
        val addExpenseMethod = botFunctionMethod(GeminiFunction.ADD_EXPENSE, Double::class.java, String::class.java, String::class.java, String::class.java, String::class.java)
        val addIncomeMethod = botFunctionMethod(GeminiFunction.ADD_INCOME, Double::class.java, String::class.java, String::class.java, String::class.java, String::class.java)
        val outOfContextMethod = botFunctionMethod(GeminiFunction.OUT_OF_CONTEXT, String::class.java)

        // Category functions
        val createCategoryMethod = botFunctionMethod(GeminiFunction.CREATE_CATEGORY, String::class.java, String::class.java, String::class.java)
        val deleteCategoryMethod = botFunctionMethod(GeminiFunction.DELETE_CATEGORY, String::class.java, String::class.java)
        val renameCategoryMethod = botFunctionMethod(GeminiFunction.RENAME_CATEGORY, String::class.java, String::class.java, String::class.java)
        val changeCategoryIconMethod = botFunctionMethod(GeminiFunction.CHANGE_CATEGORY_ICON, String::class.java, String::class.java, String::class.java)
        val deleteAllCategoriesMethod = botFunctionMethod(GeminiFunction.DELETE_ALL_CATEGORIES)
        val listCategoriesMethod = botFunctionMethod(GeminiFunction.LIST_CATEGORIES, String::class.java)

        // Group / balance / report / history / notification functions
        val listGroupsMethod = botFunctionMethod(GeminiFunction.LIST_GROUPS)
        val switchGroupMethod = botFunctionMethod(GeminiFunction.SWITCH_GROUP, String::class.java)
        val showBalanceMethod = botFunctionMethod(GeminiFunction.SHOW_BALANCE)
        val showReportMethod = botFunctionMethod(GeminiFunction.SHOW_REPORT, Double::class.javaObjectType, Double::class.javaObjectType)
        val showHistoryMethod = botFunctionMethod(GeminiFunction.SHOW_HISTORY, String::class.java, String::class.java, String::class.java, String::class.java)
        val listNotificationsMethod = botFunctionMethod(GeminiFunction.LIST_NOTIFICATIONS)
        val createNotificationMethod = botFunctionMethod(GeminiFunction.CREATE_NOTIFICATION, String::class.java, Double::class.java, Double::class.javaObjectType)
        val deleteNotificationMethod = botFunctionMethod(GeminiFunction.DELETE_NOTIFICATION, String::class.java)

        val systemContent = Content.fromParts(Part.fromText(aiPromptService.systemPrompt))

        config = GenerateContentConfig.builder()
            .tools(
                Tool.builder()
                    .functions(
                        createGroupMethod, deleteGroupMethod, addExpenseMethod, addIncomeMethod, outOfContextMethod,
                        createCategoryMethod, deleteCategoryMethod, renameCategoryMethod,
                        changeCategoryIconMethod, deleteAllCategoriesMethod, listCategoriesMethod,
                        listGroupsMethod, switchGroupMethod, showBalanceMethod, showReportMethod, showHistoryMethod,
                        listNotificationsMethod, createNotificationMethod, deleteNotificationMethod
                    )
                    .build()
            )
            .systemInstruction(systemContent)
            .automaticFunctionCalling(
                AutomaticFunctionCallingConfig.builder()
                    .disable(true)
                    .build()
            )
            // Thinking отключён: для function-calling на коротких командах он добавляет
            // 5-15 сек латентности без пользы. Включить при сложном reasoning — убрать
            // thinkingBudget или поставить >0 (например, 1024).
            .thinkingConfig(
                ThinkingConfig.builder()
                    .thinkingBudget(0)
                    .build()
            )
            .build()

    }

    private fun botFunctionMethod(function: GeminiFunction, vararg parameterTypes: Class<*>) =
        BotFunctions::class.java.getMethod(function.functionName, *parameterTypes)

    fun parseCommand(userMessage: String, categoryContext: String? = null): BotCommand =
        parseCommands(userMessage, categoryContext).first()

    /** @param audioBytes байты аудио файла (OGG/OPUS от Telegram) */
    fun parseVoiceCommand(audioBytes: ByteArray, categoryContext: String? = null): BotCommand =
        parseVoiceCommands(audioBytes, categoryContext).first()

    fun parseCommands(userMessage: String, categoryContext: String? = null): List<BotCommand> {
        return try {
            val content = contentWithCategoryContext(categoryContext, Part.fromText(userMessage))
            val response = client.models.generateContent(geminiProperties.model, content, config)
            processResponse(response, userMessage)
        } catch (e: Exception) {
            log.error("CommandParser error: ${e.message}", e)
            listOf(classifyError(e))
        }
    }

    /** @param audioBytes байты аудио файла (OGG/OPUS от Telegram) */
    fun parseVoiceCommands(audioBytes: ByteArray, categoryContext: String? = null): List<BotCommand> {
        return try {
            log.info("Processing voice message: ${audioBytes.size} bytes")

            // Создаём Part с аудио данными
            val audioPart = Part.builder()
                .inlineData(
                    Blob.builder()
                        .mimeType(VOICE_MIME_TYPE)
                        .data(audioBytes)
                        .build()
                )
                .build()

            // Добавляем инструкцию для транскрибации
            val textPart = Part.fromText(aiPromptService.voiceTranscriptionPrompt)

            val content = contentWithCategoryContext(categoryContext, audioPart, textPart)
            val response = client.models.generateContent(geminiProperties.model, content, config)

            processResponse(response, VOICE_MESSAGE_PLACEHOLDER)
        } catch (e: Exception) {
            log.error("Voice CommandParser error: ${e.message}", e)
            listOf(classifyError(e))
        }
    }

    private fun contentWithCategoryContext(categoryContext: String?, vararg parts: Part): Content {
        val allParts = buildList {
            add(Part.fromText(aiPromptService.currentDatePreamble()))
            if (!categoryContext.isNullOrBlank()) add(Part.fromText(categoryContext))
            addAll(parts)
        }
        return Content.builder().parts(allParts).build()
    }

    private fun classifyError(e: Exception): BotCommand {
        return when (e) {
            is ClientException -> {
                if (e.code() == HTTP_TOO_MANY_REQUESTS) {
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
                    BotCommand.AddExpense(
                        dto.amount, dto.category, dto.description,
                        dto.suggestedCategoryIcon, dto.operationDate.normalizedOrNull()
                    )
                }

                GeminiFunction.ADD_INCOME -> {
                    val dto = argsMapper.map<AddIncomeArgs>(args)
                    BotCommand.AddIncome(
                        dto.amount, dto.category, dto.description,
                        dto.suggestedCategoryIcon, dto.operationDate.normalizedOrNull()
                    )
                }

                GeminiFunction.OUT_OF_CONTEXT -> BotCommand.OutOfContext(originalMessage)

                GeminiFunction.CREATE_CATEGORY -> {
                    val dto = argsMapper.map<CreateCategoryArgs>(args)
                    BotCommand.CreateCategory(
                        requireText(dto.name, FIELD_CATEGORY_NAME),
                        requireText(dto.type, FIELD_CATEGORY_TYPE),
                        dto.icon.normalizedOrNull()
                    )
                }

                GeminiFunction.DELETE_CATEGORY -> {
                    val dto = argsMapper.map<DeleteCategoryArgs>(args)
                    BotCommand.DeleteCategory(requireText(dto.name, FIELD_CATEGORY_NAME), dto.type.normalizedOrNull())
                }

                GeminiFunction.RENAME_CATEGORY -> {
                    val dto = argsMapper.map<RenameCategoryArgs>(args)
                    BotCommand.RenameCategory(
                        requireText(dto.oldName, FIELD_CATEGORY_NAME),
                        requireText(dto.newName, FIELD_CATEGORY_NAME),
                        dto.type.normalizedOrNull()
                    )
                }

                GeminiFunction.CHANGE_CATEGORY_ICON -> {
                    val dto = argsMapper.map<ChangeCategoryIconArgs>(args)
                    BotCommand.ChangeCategoryIcon(
                        requireText(dto.name, FIELD_CATEGORY_NAME),
                        requireText(dto.newIcon, FIELD_CATEGORY_ICON),
                        dto.type.normalizedOrNull()
                    )
                }

                GeminiFunction.DELETE_ALL_CATEGORIES -> BotCommand.DeleteAllCategories

                GeminiFunction.LIST_CATEGORIES ->
                    BotCommand.ListCategories(argsMapper.map<ListCategoriesArgs>(args).type.normalizedOrNull())

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
                        dto.startDate.normalizedOrNull(),
                        dto.endDate.normalizedOrNull(),
                        dto.type.normalizedOrNull(),
                        dto.categoryFilter.normalizedOrNull()
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
            }
        } catch (e: Exception) {
            // Любая проблема маппинга/валидации → ParseError
            BotCommand.ParseError("Invalid function args for ${functionCall.name().orElse(UNKNOWN_FUNCTION_NAME)}: ${e.message}")
        }
    }

    private fun requireText(value: String, field: String): String {
        require(value.isNotBlank()) { "$field is blank" }
        return value.trim()
    }

    private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val VOICE_MIME_TYPE = "audio/ogg"
        private const val VOICE_MESSAGE_PLACEHOLDER = "[voice message]"
        private val RETRY_AFTER_REGEX = Regex("""Please retry in (\d+(?:\.\d+)?)s""")

        private const val FIELD_GROUP_NAME = "groupName"
        private const val FIELD_CATEGORY_NAME = "category name"
        private const val FIELD_CATEGORY_TYPE = "category type"
        private const val FIELD_CATEGORY_ICON = "category icon"
        private const val FIELD_NOTIFICATION_NAME = "notification name"
        private const val UNKNOWN_FUNCTION_NAME = "unknown"
    }
}