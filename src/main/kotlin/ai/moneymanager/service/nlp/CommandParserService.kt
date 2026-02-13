package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.nlp.BotCommand

interface CommandParserService {
    /**
     * Парсит текстовое сообщение пользователя и определяет команду
     */
    fun parseCommand(userMessage: String): BotCommand

    /**
     * Парсит голосовое сообщение и определяет команду
     * @param audioBytes байты аудио файла (OGG/OPUS от Telegram)
     */
    fun parseVoiceCommand(audioBytes: ByteArray): BotCommand
}