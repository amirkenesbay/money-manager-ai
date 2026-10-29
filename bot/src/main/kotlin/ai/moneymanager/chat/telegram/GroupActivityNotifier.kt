package ai.moneymanager.chat.telegram

import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.domain.event.CategoryDeleted
import ai.moneymanager.domain.event.CategoryRenamed
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

private const val CATEGORY_RENAMED_KEY = "group.category_notice.renamed"
private const val CATEGORY_DELETED_KEY = "group.category_notice.deleted"
private const val OPEN_CATEGORIES_BUTTON_KEY = "mini_app.button.categories"

@Service
class GroupActivityNotifier(
    private val groupService: GroupService,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
    private val miniAppGateway: MiniAppGateway,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    fun onCategoryRenamed(event: CategoryRenamed) {
        notifyOtherMembers(event.groupId, event.actorId) { language, groupName, actorName ->
            localizationService.t(
                CATEGORY_RENAMED_KEY, language, groupName,
                formatIconPrefix(event.icon) + event.oldName, formatIconPrefix(event.icon) + event.newName, actorName
            )
        }
    }

    @EventListener
    fun onCategoryDeleted(event: CategoryDeleted) {
        notifyOtherMembers(event.groupId, event.actorId) { language, groupName, actorName ->
            localizationService.t(CATEGORY_DELETED_KEY, language, groupName, formatIconPrefix(event.icon) + event.name, actorName)
        }
    }

    private fun notifyOtherMembers(groupId: ObjectId, actorId: Long, text: (String?, String, String) -> String) {
        val group = groupService.getGroup(groupId) ?: return
        val recipients = group.memberIds - actorId
        if (recipients.isEmpty()) return
        val actorName = userInfoService.displayNames(setOf(actorId)).getValue(actorId)
        recipients.forEach { memberId ->
            val language = userInfoService.getUserInfoByTelegramId(memberId)?.language
            runCatching { execute(categoryNotice(memberId, language, text(language, group.name, actorName))) }
                .onFailure { log.warn("Failed to notify member {} of group {}: {}", memberId, groupId, it.message) }
        }
    }

    private fun categoryNotice(memberId: Long, language: String?, text: String): SendMessage =
        SendMessage(memberId.toString(), text).apply {
            if (miniAppGateway.enabled) {
                replyMarkup = miniAppGateway.keyboard(OPEN_CATEGORIES_BUTTON_KEY, language, MiniAppScreen.CATEGORIES)
            }
        }
}
