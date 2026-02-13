package ai.moneymanager.service

interface GeminiService {
    /**
     * Генерирует текстовый ответ от Gemini
     */
    fun generateText(prompt: String): String?

    /**
     * Проверка работоспособности Gemini API
     */
    fun healthCheck(): Boolean
}