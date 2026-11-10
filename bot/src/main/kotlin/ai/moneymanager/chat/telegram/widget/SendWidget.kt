package ai.moneymanager.chat.telegram.widget

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard

private const val METHOD = "sendWidget"

@JsonInclude(JsonInclude.Include.NON_NULL)
class SendWidget(
    @field:JsonProperty("chat_id") val chatId: String,
    @field:JsonProperty("html") val html: String,
    @field:JsonProperty("reply_markup") val replyMarkup: ReplyKeyboard? = null,
) : BotApiMethod<Message>() {
    override fun getMethod(): String = METHOD

    override fun deserializeResponse(answer: String): Message = deserializeResponse(answer, Message::class.java)

    override fun validate() {
        require(chatId.isNotBlank() && html.isNotBlank())
    }
}
