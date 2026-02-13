package ai.moneymanager.service.impl

import ai.moneymanager.chat.config.GeminiProperties
import ai.moneymanager.service.GeminiService
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GeminiService(
    private val geminiProperties: GeminiProperties
): GeminiService {
    private lateinit var client: Client

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        client = Client.builder()
            .apiKey(geminiProperties.apiKey)
            .build()
        log.info("✅ Gemini client initialized with model: ${geminiProperties.model}")
    }

    /**
     * Генерирует текстовый ответ от Gemini
     */
    override fun generateText(prompt: String): String? {
        return try {
            val response: GenerateContentResponse = client.models.generateContent(
                geminiProperties.model,
                prompt,
                null
            )
            response.text()
        } catch (e: Exception) {
            log.info("❌ Gemini error: ${e.message}")
            null
        }
    }

    /**
     * Проверка работоспособности Gemini API
     */
    override fun healthCheck(): Boolean {
        return try {
            val response = generateText("Ответь одним словом: работает")
            response != null && response.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}