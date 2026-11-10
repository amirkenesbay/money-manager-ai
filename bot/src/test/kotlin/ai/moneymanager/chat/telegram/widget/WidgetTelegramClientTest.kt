package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.testing.anyOf
import ai.moneymanager.testing.testLocalizationService
import kz.rmr.chatmachinist.service.TelegramClient
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private const val CHAT_ID = 42L
private const val SAVED_TEXT = "✅ Расход добавлен: ☕«Кафе» — 1 500₸"
private const val WIDGET_MESSAGE_ID = 100
private const val TEXT_MESSAGE_ID = 200

class WidgetTelegramClientTest {
    private val delegate = mock(TelegramClient::class.java)
    private val sender = mock(WidgetSender::class.java)
    private val outbox = WidgetOutbox(Clock.fixed(LocalDate.of(2026, 11, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC))
    private val client = WidgetTelegramClient(delegate, { outbox }, { OperationCardRenderer(testLocalizationService()) }, { sender })
    private val cafe = Category(ObjectId(), "Кафе", "☕", CategoryType.EXPENSE, ObjectId())
    private val today = LocalDate.of(2026, 11, 10)

    private fun queueCard() =
        outbox.add(CHAT_ID, OperationCard(SAVED_TEXT, CategoryType.EXPENSE, BigDecimal("1500"), Currency.KZT, cafe, null, today, today, null, "ru"))

    private fun message(text: String) = SendMessage(CHAT_ID.toString(), text)

    @Test
    fun `sends the saved operation as a card`() {
        queueCard()
        `when`(sender.send(anyString(), anyString(), anyOf(SendMessage::class.java))).thenReturn(WIDGET_MESSAGE_ID)

        assertThat(client.sendMessage(message(SAVED_TEXT))).isEqualTo(WIDGET_MESSAGE_ID)
        verify(delegate, never()).sendMessage(anyOf(SendMessage::class.java))
    }

    @Test
    fun `falls back to text when telegram refuses the card`() {
        queueCard()
        `when`(sender.send(anyString(), anyString(), anyOf(SendMessage::class.java))).thenThrow(IllegalStateException("Bad Request"))
        `when`(delegate.sendMessage(anyOf(SendMessage::class.java))).thenReturn(TEXT_MESSAGE_ID)

        assertThat(client.sendMessage(message(SAVED_TEXT))).isEqualTo(TEXT_MESSAGE_ID)
    }

    @Test
    fun `leaves any other message as text`() {
        queueCard()
        `when`(delegate.sendMessage(anyOf(SendMessage::class.java))).thenReturn(TEXT_MESSAGE_ID)

        assertThat(client.sendMessage(message("📋 Меню"))).isEqualTo(TEXT_MESSAGE_ID)
        verify(sender, never()).send(anyString(), anyString(), anyOf(SendMessage::class.java))
    }

    @Test
    fun `does not touch an edit of the same message number in another chat`() {
        queueCard()
        `when`(sender.send(anyString(), anyString(), anyOf(SendMessage::class.java))).thenReturn(WIDGET_MESSAGE_ID)
        client.sendMessage(message(SAVED_TEXT))
        val elsewhere = EditMessageText.builder().chatId(CHAT_ID + 1).messageId(WIDGET_MESSAGE_ID).text("📋 Меню").build()

        client.editMessage(elsewhere)

        verify(delegate).editMessage(elsewhere)
        verify(delegate, never()).sendMessage(anyOf(SendMessage::class.java))
    }

    @Test
    fun `answers an edit of the card with a new text message and keeps editing it`() {
        queueCard()
        `when`(sender.send(anyString(), anyString(), anyOf(SendMessage::class.java))).thenReturn(WIDGET_MESSAGE_ID)
        `when`(delegate.sendMessage(anyOf(SendMessage::class.java))).thenReturn(TEXT_MESSAGE_ID)
        client.sendMessage(message(SAVED_TEXT))

        client.editMessage(EditMessageText.builder().chatId(CHAT_ID).messageId(WIDGET_MESSAGE_ID).text("📋 Меню").build())
        val second = EditMessageText.builder().chatId(CHAT_ID).messageId(WIDGET_MESSAGE_ID).text("💰 Баланс").build()
        client.editMessage(second)

        verify(delegate).sendMessage(anyOf(SendMessage::class.java))
        assertThat(second.messageId).isEqualTo(TEXT_MESSAGE_ID)
        verify(delegate).editMessage(second)
    }
}
