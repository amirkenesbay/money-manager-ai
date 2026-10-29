package ai.moneymanager.chat.telegram

import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

private const val APP_URL = "https://app.example.com"
private const val BOT_TOKEN = "123456:TEST"
private const val ADD_OPERATION_BUTTON_KEY = "mini_app.button.add_operation"

class MiniAppGatewayTest {
    private val localization = testLocalizationService()

    @Test
    fun `opens the home screen at the bare app url`() {
        assertThat(miniAppUrl(APP_URL, MiniAppScreen.HOME)).isEqualTo(APP_URL)
    }

    @Test
    fun `passes the target screen as a query parameter`() {
        assertThat(miniAppUrl(APP_URL, MiniAppScreen.ADD_OPERATION)).isEqualTo("$APP_URL?screen=add")
    }

    @Test
    fun `builds a single Mini App button for the screen`() {
        val keyboard = gateway(enabled = true, url = APP_URL).keyboard(ADD_OPERATION_BUTTON_KEY, "ru", MiniAppScreen.ADD_OPERATION)

        val button = keyboard.keyboard.single().single()
        assertThat(button.text).isEqualTo(localization.t(ADD_OPERATION_BUTTON_KEY, "ru"))
        assertThat(button.webApp.url).isEqualTo("$APP_URL?screen=add")
    }

    @Test
    fun `refuses to turn the gateway on without an https url`() {
        assertThatThrownBy { gateway(enabled = true, url = "") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `starts with the gateway off and no url`() {
        assertThat(gateway(enabled = false, url = "").enabled).isFalse()
    }

    private fun gateway(enabled: Boolean, url: String): MiniAppGateway {
        val properties = MiniAppProperties().apply {
            this.url = url
            botGateway = enabled
        }
        return MiniAppGateway(properties, mock(UserInfoService::class.java), localization, BOT_TOKEN)
    }
}
