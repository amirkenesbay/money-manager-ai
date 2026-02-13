package ai.moneymanager.service.nlp.impl

import ai.moneymanager.chat.config.GeminiProperties
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.BotFunctions
import ai.moneymanager.domain.model.nlp.arguments.AddExpenseArgs
import ai.moneymanager.domain.model.nlp.arguments.AddIncomeArgs
import ai.moneymanager.domain.model.nlp.arguments.CreateGroupArgs
import ai.moneymanager.domain.model.nlp.enum.GeminiFunction
import ai.moneymanager.mapper.GeminiArgsMapper
import ai.moneymanager.service.nlp.CommandParserService
import com.google.genai.Client
import com.google.genai.types.AutomaticFunctionCallingConfig
import com.google.genai.types.Blob
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import com.google.genai.types.Tool
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandParserServiceImpl(
    private val geminiProperties: GeminiProperties,
    private val argsMapper: GeminiArgsMapper
): CommandParserService {
    private lateinit var client: Client
    private lateinit var config: GenerateContentConfig

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        client = Client.builder()
            .apiKey(geminiProperties.apiKey)
            .build()

        // Получаем методы из BotFunctions для function calling
        val createGroupMethod = BotFunctions::class.java.getMethod("createGroup", String::class.java)
        val addExpenseMethod = BotFunctions::class.java.getMethod("addExpense", Double::class.java, String::class.java, String::class.java)
        val addIncomeMethod = BotFunctions::class.java.getMethod("addIncome", Double::class.java, String::class.java, String::class.java)
        val outOfContextMethod = BotFunctions::class.java.getMethod("outOfContext", String::class.java)

        val systemContent = Content.fromParts(Part.fromText(SYSTEM_PROMPT))

        config = GenerateContentConfig.builder()
            .tools(
                Tool.builder()
                    .functions(createGroupMethod, addExpenseMethod, addIncomeMethod, outOfContextMethod)
                    .build()
            )
            .systemInstruction(systemContent)
            .automaticFunctionCalling(
                AutomaticFunctionCallingConfig.builder()
                    .disable(true)
                    .build()
            )
            .build()

    }

    /**
     * Парсит текстовое сообщение пользователя и определяет команду
     */
    override fun parseCommand(userMessage: String): BotCommand {
        return try {
            val response = client.models.generateContent(
                geminiProperties.model,
                userMessage,
                config
            )
            processResponse(response, userMessage)
        } catch (e: Exception) {
            log.info("❌ CommandParser error: ${e.message}")
            e.printStackTrace()
            BotCommand.ParseError(e.message ?: "Unknown error")
        }
    }

    /**
     * Парсит голосовое сообщение и определяет команду
     * @param audioBytes байты аудио файла (OGG/OPUS от Telegram)
     */
    override fun parseVoiceCommand(audioBytes: ByteArray): BotCommand {
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
            val textPart = Part.fromText("Распознай речь и определи намерение пользователя.")

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
            log.info("❌ Voice CommandParser error: ${e.message}")
            e.printStackTrace()
            BotCommand.ParseError(e.message ?: "Unknown error")
        }
    }

    private fun processResponse(response: GenerateContentResponse, originalMessage: String): BotCommand {
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
        val function = GeminiFunction.Companion.from(functionCall.name().orElse(null))

        return try {
            when (function) {
                GeminiFunction.CREATE_GROUP -> {
                    val dto = argsMapper.map<CreateGroupArgs>(args)
                    // Можно добавить простую валидацию:
                    if (dto.groupName.isBlank()) return BotCommand.ParseError("groupName is blank")
                    BotCommand.CreateGroup(dto.groupName)
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
        private val SYSTEM_PROMPT = """
            Ты — ассистент Telegram бота для учета финансов. Твоя задача — понять намерение пользователя и вызвать соответствующую функцию.
            
            Примеры сообщений:                                                                                                                                             
            - создать группу для совместного учета. Примеры: "создай группу друзья", "новая группа семья" → createGroup                                                                                                                         
            - добавить расход. Примеры: "купил кофе 500", "потратил 1000 на такси" → addExpense (amount=500, category="Кофе")                                                                                                  
            - добавить доход. Примеры: "получил зарплату 500000", "подарили 10000" → addIncome                                                                                                                        
            - если сообщение НЕ относится к финансам или управлению группами → outOfContext 

            ВАЖНО:
            - Если пользователь спрашивает что-то не по теме (математика, погода, общие вопросы) — вызови outOfContext
            - Для расходов/доходов определи категорию из контекста (продукты, транспорт, зарплата и т.д.)
            - Валюта по умолчанию — тенге (KZT)
            - Всегда вызывай одну из функций, не отвечай текстом
            - Никогда не отвечай текстом
            - Всегда возвращай function call
            - Если сомневаешься — outOfContext
            - Если сумма не указана — outOfContext
        """.trimIndent()
    }
}