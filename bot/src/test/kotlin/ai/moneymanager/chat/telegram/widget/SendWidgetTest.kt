package ai.moneymanager.chat.telegram.widget

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

class SendWidgetTest {
    private val mapper = ObjectMapper()

    @Test
    fun `serializes to the bot api fields`() {
        val keyboard = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton.builder().text("Меню").callbackData("menu").build())))

        val json = mapper.readTree(mapper.writeValueAsString(SendWidget("42", "<b>1</b>", keyboard)))

        assertThat(json["chat_id"].asText()).isEqualTo("42")
        assertThat(json["html"].asText()).isEqualTo("<b>1</b>")
        assertThat(json["reply_markup"]["inline_keyboard"][0][0]["callback_data"].asText()).isEqualTo("menu")
        assertThat(SendWidget("42", "<b>1</b>").method).isEqualTo("sendWidget")
    }

    @Test
    fun `leaves out a missing keyboard`() {
        assertThat(mapper.readTree(mapper.writeValueAsString(SendWidget("42", "<b>1</b>"))).has("reply_markup")).isFalse()
    }

    @Test
    fun `reads the sent message from the answer`() {
        val answer = """{"ok":true,"result":{"message_id":7,"date":1,"chat":{"id":42,"type":"private"},"widget":{"html":"<b>1</b>"}}}"""

        assertThat(SendWidget("42", "<b>1</b>").deserializeResponse(answer).messageId).isEqualTo(7)
    }
}
