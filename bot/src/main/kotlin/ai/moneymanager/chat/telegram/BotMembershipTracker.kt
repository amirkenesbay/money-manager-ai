package ai.moneymanager.chat.telegram

import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.model.UpdateResponse
import kz.rmr.chatmachinist.service.UpdateHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated
import org.telegram.telegrambots.meta.api.objects.Update

private const val PRIVATE_CHAT = "private"
private const val STATUS_BLOCKED = "kicked"
private const val STATUS_MEMBER = "member"

@Component
class BotMembershipTracker(private val userInfoService: ObjectProvider<UserInfoService>) : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any =
        if (bean is UpdateHandler) MembershipAwareUpdateHandler(bean, this) else bean

    fun onMembership(change: ChatMemberUpdated) {
        if (change.chat?.type != PRIVATE_CHAT) return
        val blocked = when (change.newChatMember?.status) {
            STATUS_BLOCKED -> true
            STATUS_MEMBER -> false
            else -> return
        }
        userInfoService.getObject().markBotBlocked(change.from.id, blocked)
    }
}

private class MembershipAwareUpdateHandler(
    private val delegate: UpdateHandler,
    private val tracker: BotMembershipTracker,
) : UpdateHandler {
    override fun handle(update: Update): List<UpdateResponse> {
        val membership = update.myChatMember ?: return delegate.handle(update)
        tracker.onMembership(membership)
        return emptyList()
    }
}
