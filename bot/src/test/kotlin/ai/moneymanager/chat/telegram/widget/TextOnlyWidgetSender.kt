package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.testing.anyOf
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

fun anyFallback(): () -> Int {
    Mockito.any(Function0::class.java)
    return { 0 }
}

@Suppress("UNCHECKED_CAST")
fun textOnlyWidgetSender(): WidgetSender = mock(WidgetSender::class.java).also { sender ->
    `when`(sender.sendCard(anyOf(SendMessage::class.java), anyString(), anyFallback())).thenAnswer { (it.getArgument<Any>(2) as () -> Int)() }
}
