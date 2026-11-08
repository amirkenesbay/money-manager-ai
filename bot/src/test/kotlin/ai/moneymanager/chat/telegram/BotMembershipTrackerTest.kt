package ai.moneymanager.chat.telegram

import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.service.UpdateHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.ObjectProvider
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember

private const val USER_ID = 42L
private const val PRIVATE = "private"
private const val GROUP = "group"

class BotMembershipTrackerTest {
    private val userInfoService = mock(UserInfoService::class.java)
    private val delegate = mock(UpdateHandler::class.java)
    private val tracker = BotMembershipTracker(provider(userInfoService))
    private val handler = tracker.postProcessAfterInitialization(delegate, "updateHandler") as UpdateHandler

    private fun membership(member: ChatMember, chatType: String = PRIVATE) = Update().apply {
        myChatMember = ChatMemberUpdated().apply {
            chat = Chat(USER_ID, chatType)
            from = User(USER_ID, "Dev", false)
            newChatMember = member
        }
    }

    @Test
    fun `marks the user when they block the bot and keeps the update away from dialogs`() {
        val responses = handler.handle(membership(ChatMemberBanned()))

        verify(userInfoService).markBotBlocked(USER_ID, true)
        verifyNoInteractions(delegate)
        assertThat(responses).isEmpty()
    }

    @Test
    fun `clears the mark when they unblock the bot`() {
        handler.handle(membership(ChatMemberMember()))

        verify(userInfoService).markBotBlocked(USER_ID, false)
    }

    @Test
    fun `ignores membership changes in group chats`() {
        handler.handle(membership(ChatMemberBanned(), GROUP))

        verify(userInfoService, never()).markBotBlocked(USER_ID, true)
    }

    @Test
    fun `passes every other update to the dialogs`() {
        val update = Update()

        handler.handle(update)

        verify(delegate).handle(update)
    }

    private fun <T : Any> provider(value: T): ObjectProvider<T> = object : ObjectProvider<T> {
        override fun getObject(): T = value
        override fun getObject(vararg args: Any?): T = value
        override fun getIfAvailable(): T = value
        override fun getIfUnique(): T = value
    }
}
