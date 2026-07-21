package ai.moneymanager.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand

private const val START_COMMAND = "start"
private const val START_DESCRIPTION = "Открыть главное меню"

/** Регистрирует меню команд (кнопка рядом с полем ввода) — вместо ручного набора /start. */
@Service
class BotCommandMenuService(
    @Value("\${chat-machinist.bot.token}")
    botToken: String
) : DefaultAbsSender(DefaultBotOptions(), botToken) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun registerCommands() {
        runCatching {
            execute(SetMyCommands(listOf(BotCommand(START_COMMAND, START_DESCRIPTION)), null, null))
        }.onFailure {
            log.warn("Failed to register bot commands menu: ${it.message}")
        }
    }
}
