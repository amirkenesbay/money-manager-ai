package ai.moneymanager.chat.config

import ai.moneymanager.chat.reply.category.*
import ai.moneymanager.chat.reply.group.*
import ai.moneymanager.chat.reply.money.menuReply
import ai.moneymanager.chat.reply.nlp.nlpConfirmCreateGroupReply
import ai.moneymanager.chat.reply.nlp.nlpResponseReply
import ai.moneymanager.chat.reply.notification.*
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.NotificationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder
import kz.rmr.chatmachinist.api.reply.replies
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MoneyManagerReplyConfig(
    private val notificationService: NotificationService
) {
    @Bean
    fun replyBuilder(): RepliesBuilder<MoneyManagerState, MoneyManagerContext> {
        return replies {
            chatName = "Money Manager Chat"

            // Money replies
            menuReply()

            // Group replies
            groupManagementReply()
            groupCreateEnterNameReply()
            groupInviteShowReply()
            groupInviteSelectReply()
            groupJoinConfirmReply()
            groupListReply()
            groupMembersReply()
            groupEditSelectReply()
            groupEditEnterNameReply()
            groupDeleteSelectReply()
            groupDeleteConfirmReply()

            // Category replies
            categoryManagementReply()
            categoryNoGroupWarningReply()
            categoryCreateSelectTypeReply()
            categoryCreateEnterNameReply()
            categoryListReply()
            categoryActionsReply()
            categoryEditNameReply()
            categoryDeleteConfirmReply()
            categoryDeleteAllConfirmReply()

            // NLP replies
            nlpConfirmCreateGroupReply()
            nlpResponseReply()

            // Notification replies
            notificationListReply(notificationService)
            notificationCreateEnterNameReply()
            notificationCreateSelectPeriodReply()
            notificationCreateCustomEnterNReply()
            notificationCreateCustomSelectUnitReply()
            notificationCreateSelectDayOfWeekReply()
            notificationCreateEnterDayOfMonthReply()
            notificationCreateSelectMonthReply()
            notificationCreateEnterTimeReply()
            notificationCreateConfirmReply(notificationService)
            notificationEditSelectReply(notificationService)
            notificationEditMenuReply(notificationService)
            notificationEditEnterNameReply()
            notificationEditSelectPeriodReply()
            notificationEditCustomEnterNReply()
            notificationEditCustomSelectUnitReply()
            notificationEditSelectDayOfWeekReply()
            notificationEditEnterDayOfMonthReply()
            notificationEditSelectMonthReply()
            notificationEditEnterTimeReply()
            notificationEditConfirmReply(notificationService)
            notificationDeleteSelectReply(notificationService)
            notificationDeleteConfirmReply(notificationService)
            notificationDeleteAllConfirmReply()
        }
    }
}
