package ai.moneymanager.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

private const val MENU_BUTTON_TEXT = "▶️ Открыть меню"
private const val ATTACH_MESSAGE_TEXT = "👇"

/**
 * Постоянная reply-кнопка рядом с системной клавиатурой — отдельный UI-слой от основного
 * inline-интерфейса бота. Крепится один раз при /start, дальше просто всегда видна.
 */
@Service
class PersistentMenuKeyboardService(
    @Value("\${chat-machinist.bot.token}")
    botToken: String
) : DefaultAbsSender(DefaultBotOptions(), botToken) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun attach(chatId: Long) {
        val keyboard = ReplyKeyboardMarkup().apply {
            keyboard = listOf(KeyboardRow(listOf(KeyboardButton(MENU_BUTTON_TEXT))))
            resizeKeyboard = true
            isPersistent = true
        }
        val message = SendMessage(chatId.toString(), ATTACH_MESSAGE_TEXT).apply {
            replyMarkup = keyboard
        }

        runCatching {
            execute(message)
        }.onFailure {
            log.warn("Failed to attach persistent menu keyboard: ${it.message}")
        }
    }
}
