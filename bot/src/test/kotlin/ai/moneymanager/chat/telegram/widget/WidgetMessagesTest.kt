package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.service.LocalizationService
import ai.moneymanager.testing.testLocalizationService
import kz.rmr.chatmachinist.service.TelegramClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.MapPropertySource
import java.time.Clock

private const val TOKEN_PROPERTY = "chat-machinist.bot.token"
private const val TEST_TOKEN = "123456:TEST-BOT-TOKEN"

class WidgetMessagesTest {
    @Configuration
    @Import(WidgetMessages::class, WidgetOutbox::class, OperationCardRenderer::class, WidgetSender::class)
    class Beans {
        @Bean
        fun telegramClient(): TelegramClient = mock(TelegramClient::class.java)

        @Bean
        fun localizationService(): LocalizationService = testLocalizationService()

        @Bean
        fun clock(): Clock = Clock.systemUTC()

        companion object {
            @Bean
            @JvmStatic
            fun placeholders() = PropertySourcesPlaceholderConfigurer()
        }
    }

    @Test
    fun `wraps the chat machinist client so replies can become cards`() {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(MapPropertySource("test", mapOf(TOKEN_PROPERTY to TEST_TOKEN)))
            context.register(Beans::class.java)
            context.refresh()

            assertThat(context.getBean(TelegramClient::class.java)).isInstanceOf(WidgetTelegramClient::class.java)
        }
    }
}
