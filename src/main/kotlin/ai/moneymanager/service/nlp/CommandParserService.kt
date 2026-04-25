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
import ai.moneymanager.domain.model.nlp.arguments.DeleteCategoryArgs
import ai.moneymanager.domain.model.nlp.arguments.DeleteGroupArgs
import ai.moneymanager.domain.model.nlp.arguments.ListCategoriesArgs
import ai.moneymanager.domain.model.nlp.arguments.RenameCategoryArgs
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
        val addExpenseMethod = botFunctionMethod(GeminiFunction.ADD_EXPENSE, Double::class.java, String::class.java, String::class.java)
        val addIncomeMethod = botFunctionMethod(GeminiFunction.ADD_INCOME, Double::class.java, String::class.java, String::class.java)
        val outOfContextMethod = botFunctionMethod(GeminiFunction.OUT_OF_CONTEXT, String::class.java)

        // Category functions
        val createCategoryMethod = botFunctionMethod(GeminiFunction.CREATE_CATEGORY, String::class.java, String::class.java, String::class.java)
        val deleteCategoryMethod = botFunctionMethod(GeminiFunction.DELETE_CATEGORY, String::class.java, String::class.java)
        val renameCategoryMethod = botFunctionMethod(GeminiFunction.RENAME_CATEGORY, String::class.java, String::class.java, String::class.java)
        val changeCategoryIconMethod = botFunctionMethod(GeminiFunction.CHANGE_CATEGORY_ICON, String::class.java, String::class.java, String::class.java)
        val deleteAllCategoriesMethod = botFunctionMethod(GeminiFunction.DELETE_ALL_CATEGORIES)
        val listCategoriesMethod = botFunctionMethod(GeminiFunction.LIST_CATEGORIES, String::class.java)

        val systemContent = Content.fromParts(Part.fromText(aiPromptService.systemPrompt))

        config = GenerateContentConfig.builder()
            .tools(
                Tool.builder()
                    .functions(
                        createGroupMethod, deleteGroupMethod, addExpenseMethod, addIncomeMethod, outOfContextMethod,
                        createCategoryMethod, deleteCategoryMethod, renameCategoryMethod,
                        changeCategoryIconMethod, deleteAllCategoriesMethod, listCategoriesMethod
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

    fun parseCommand(userMessage: String): BotCommand {
        return try {
            val response = client.models.generateContent(
                geminiProperties.model,
                userMessage,
                config
            )
            processResponse(response, userMessage)
        } catch (e: Exception) {
            log.error("CommandParser error: ${e.message}", e)
            classifyError(e)
        }
    }

    /** @param audioBytes байты аудио файла (OGG/OPUS от Telegram) */
    fun parseVoiceCommand(audioBytes: ByteArray): BotCommand {
        return try {
            log.info("🎤 Processing voice message: ${audioBytes.size} bytes")

            // Создаём Part с аудио данными
            val audioPart = Part.builder()
                .inlineData(
                    Blob.builder()
                        .mimeType("audio/ogg")
                        .data(audioBytes)
                        .build()
                )
                .build()

            // Добавляем инструкцию для транскрибации
            val textPart = Part.fromText(aiPromptService.voiceTranscriptionPrompt)

            val content = Content.builder()
                .parts(listOf(audioPart, textPart))
                .build()

            val response = client.models.generateContent(
                geminiProperties.model,
                content,
                config
            )

            processResponse(response, "[voice message]")
        } catch (e: Exception) {
            log.error("Voice CommandParser error: ${e.message}", e)
            classifyError(e)
        }
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

    private fun processResponse(response: com.google.genai.types.GenerateContentResponse, originalMessage: String): BotCommand {
        val functionCall = response.functionCalls()?.firstOrNull()

        return if (functionCall != null) {
            log.info("🤖 Gemini function call: ${functionCall.name()} with args: ${functionCall.args()}")
            parseFunctionCall(functionCall, originalMessage)
        } else {
            val responseText = response.text()
            log.info("🤖 Gemini text response: $responseText")
            BotCommand.OutOfContext(originalMessage)
        }
    }

    private fun parseFunctionCall(functionCall: FunctionCall, originalMessage: String): BotCommand {

        val args = functionCall.args().orElse(emptyMap())
        val function = GeminiFunction.from(functionCall.name().orElse(null))

        return try {
            when (function) {
                GeminiFunction.CREATE_GROUP -> {
                    val dto = argsMapper.map<CreateGroupArgs>(args)
                    // Можно добавить простую валидацию:
                    if (dto.groupName.isBlank()) return BotCommand.ParseError("groupName is blank")
                    BotCommand.CreateGroup(dto.groupName)
                }

                GeminiFunction.DELETE_GROUP -> {
                    val dto = argsMapper.map<DeleteGroupArgs>(args)
                    if (dto.groupName.isBlank()) return BotCommand.ParseError("groupName is blank")
                    BotCommand.DeleteGroup(dto.groupName)
                }

                GeminiFunction.ADD_EXPENSE -> {
                    val dto = argsMapper.map<AddExpenseArgs>(args)
                    BotCommand.AddExpense(dto.amount, dto.category, dto.description)
                }

                GeminiFunction.ADD_INCOME -> {
                    val dto = argsMapper.map<AddIncomeArgs>(args)
                    BotCommand.AddIncome(dto.amount, dto.category, dto.description)
                }

                GeminiFunction.OUT_OF_CONTEXT -> {
                    // можно мапить, а можно просто использовать originalMessage
                    // val dto = argsMapper.map<OutOfContextArgs>(args)
                    BotCommand.OutOfContext(originalMessage)
                }

                GeminiFunction.CREATE_CATEGORY -> {
                    val dto = argsMapper.map<CreateCategoryArgs>(args)
                    if (dto.name.isBlank()) return BotCommand.ParseError("category name is blank")
                    if (dto.type.isBlank()) return BotCommand.ParseError("category type is blank")
                    BotCommand.CreateCategory(dto.name.trim(), dto.type.trim(), dto.icon?.trim()?.takeIf { it.isNotEmpty() })
                }

                GeminiFunction.DELETE_CATEGORY -> {
                    val dto = argsMapper.map<DeleteCategoryArgs>(args)
                    if (dto.name.isBlank()) return BotCommand.ParseError("category name is blank")
                    BotCommand.DeleteCategory(dto.name.trim(), dto.type?.trim()?.takeIf { it.isNotEmpty() })
                }

                GeminiFunction.RENAME_CATEGORY -> {
                    val dto = argsMapper.map<RenameCategoryArgs>(args)
                    if (dto.oldName.isBlank() || dto.newName.isBlank()) return BotCommand.ParseError("category name is blank")
                    BotCommand.RenameCategory(dto.oldName.trim(), dto.newName.trim(), dto.type?.trim()?.takeIf { it.isNotEmpty() })
                }

                GeminiFunction.CHANGE_CATEGORY_ICON -> {
                    val dto = argsMapper.map<ChangeCategoryIconArgs>(args)
                    if (dto.name.isBlank() || dto.newIcon.isBlank()) return BotCommand.ParseError("category fields are blank")
                    BotCommand.ChangeCategoryIcon(dto.name.trim(), dto.newIcon.trim(), dto.type?.trim()?.takeIf { it.isNotEmpty() })
                }

                GeminiFunction.DELETE_ALL_CATEGORIES -> {
                    BotCommand.DeleteAllCategories
                }

                GeminiFunction.LIST_CATEGORIES -> {
                    val dto = argsMapper.map<ListCategoriesArgs>(args)
                    BotCommand.ListCategories(dto.type?.trim()?.takeIf { it.isNotEmpty() })
                }

                null -> {
                    BotCommand.OutOfContext(originalMessage)
                }
            }
        } catch (e: Exception) {
            // Любая проблема маппинга/типов → ParseError (или OutOfContext, как хочешь)
            BotCommand.ParseError("Invalid function args for ${functionCall.name().orElse("unknown")}: ${e.message}")
        }
    }

    companion object {
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val RETRY_AFTER_REGEX = Regex("""Please retry in (\d+(?:\.\d+)?)s""")
    }
}