package ai.moneymanager.chat.telegram

import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonDefault
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import java.time.YearMonth

private const val HTTPS_PREFIX = "https://"
private const val MISSING_URL_MESSAGE = "mini-app.url must be an https URL when mini-app.bot-gateway is on"
private const val MENU_BUTTON_KEY = "mini_app.menu_button"
private const val OPEN_BUTTON_KEY = "mini_app.button.open"
private const val INTRO_KEY = "mini_app.intro"
private const val HINT_KEY = "mini_app.hint"
private const val JOINED_KEY = "mini_app.joined"

@Service
class MiniAppGateway(
    private val properties: MiniAppProperties,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    init {
        require(!properties.botGateway || properties.url.startsWith(HTTPS_PREFIX)) { MISSING_URL_MESSAGE }
    }

    val enabled: Boolean
        get() = properties.botGateway

    fun keyboard(textKey: String, language: String?, screen: MiniAppScreen): InlineKeyboardMarkup =
        InlineKeyboardMarkup(listOf(listOf(button(textKey, language, screen))))

    fun button(textKey: String, language: String?, screen: MiniAppScreen, month: YearMonth? = null): InlineKeyboardButton =
        InlineKeyboardButton(localizationService.t(textKey, language)).apply {
            webApp = WebAppInfo(miniAppUrl(properties.url, screen, month))
        }

    fun sendHint(chatId: Long, telegramUserId: Long) {
        replyWithOpenButton(chatId, telegramUserId) { language -> localizationService.t(HINT_KEY, language) }
    }

    fun sendJoined(chatId: Long, telegramUserId: Long, groupName: String) {
        replyWithOpenButton(chatId, telegramUserId) { language -> localizationService.t(JOINED_KEY, language, groupName) }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun configureMenuButton() {
        val menuButton = if (enabled) {
            MenuButtonWebApp.builder()
                .text(localizationService.t(MENU_BUTTON_KEY, null))
                .webAppInfo(WebAppInfo(properties.url))
                .build()
        } else {
            MenuButtonDefault.builder().build()
        }
        runCatching { execute(SetChatMenuButton.builder().menuButton(menuButton).build()) }
            .onFailure { log.warn("Failed to configure chat menu button: {}", it.message) }
    }

    private fun replyWithOpenButton(chatId: Long, telegramUserId: Long, text: (String?) -> String) {
        val userInfo = userInfoService.getUserInfoByTelegramId(telegramUserId)
        val language = userInfo?.language
        if (userInfo?.miniAppIntroduced != true) {
            send(chatId, localizationService.t(INTRO_KEY, language), ReplyKeyboardRemove(true))
            userInfoService.markMiniAppIntroduced(telegramUserId)
        }
        send(chatId, text(language), keyboard(OPEN_BUTTON_KEY, language, MiniAppScreen.HOME))
    }

    private fun send(chatId: Long, text: String, markup: ReplyKeyboard) {
        runCatching { execute(SendMessage(chatId.toString(), text).apply { replyMarkup = markup }) }
            .onFailure { log.warn("Failed to send Mini App message to chat {}: {}", chatId, it.message) }
    }
}
