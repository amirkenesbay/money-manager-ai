package ai.moneymanager.chat.config

import ai.moneymanager.chat.reply.category.*
import ai.moneymanager.chat.reply.group.*
import ai.moneymanager.chat.reply.money.menuReply
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder
import kz.rmr.chatmachinist.api.reply.replies
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MoneyManagerReplyConfig {
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
        }
    }
}