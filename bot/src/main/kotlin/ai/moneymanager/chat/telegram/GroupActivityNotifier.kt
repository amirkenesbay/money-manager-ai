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
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.event.OperationsChanged
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.service.BudgetAlert
import ai.moneymanager.service.BudgetAlertService

private const val CATEGORY_RENAMED_KEY = "group.category_notice.renamed"
private const val CATEGORY_DELETED_KEY = "group.category_notice.deleted"
private const val OPEN_CATEGORIES_BUTTON_KEY = "mini_app.button.categories"
private const val OPEN_BUDGETS_BUTTON_KEY = "mini_app.button.budgets"
private const val BUDGET_NEAR_KEY = "budget.notice.near"
private const val BUDGET_OVER_KEY = "budget.notice.over"

@Service
class GroupActivityNotifier(
    private val groupService: GroupService,
    private val budgetAlertService: BudgetAlertService,
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

    @EventListener
    fun onOperationsChanged(event: OperationsChanged) {
        runCatching {
            val group = groupService.getGroup(event.groupId) ?: return
            budgetAlertService.claimNewAlerts(group).forEach { alert ->
                notifyMembers(group, group.memberIds, OPEN_BUDGETS_BUTTON_KEY) { language -> budgetNotice(alert, group, language) }
            }
        }.onFailure { log.warn("Failed to check budgets of group {}: {}", event.groupId, it.message) }
    }

    private fun budgetNotice(alert: BudgetAlert, group: MoneyGroup, language: String?): String {
        val budget = alert.budget
        val category = formatIconPrefix(budget.category.icon) + budget.category.name
        val spent = formatAmount(budget.spent, group.currency)
        val limit = formatAmount(budget.limit, group.currency)
        return when (alert.threshold) {
            BudgetThreshold.NEAR -> localizationService.t(
                BUDGET_NEAR_KEY, language, category, budget.percent, group.name, spent, limit,
                budget.period.end.format(shortDateFormatter), formatAmount(budget.remaining, group.currency),
            )
            BudgetThreshold.OVER -> localizationService.t(
                BUDGET_OVER_KEY, language, category, group.name, spent, limit,
                formatAmount(budget.remaining.negate(), group.currency), budget.period.end.plusDays(1).format(shortDateFormatter),
            )
        }
    }

    private fun notifyOtherMembers(groupId: ObjectId, actorId: Long, text: (String?, String, String) -> String) {
        val group = groupService.getGroup(groupId) ?: return
        val actorName = userInfoService.displayNames(setOf(actorId)).getValue(actorId)
        notifyMembers(group, group.memberIds - actorId, OPEN_CATEGORIES_BUTTON_KEY) { language -> text(language, group.name, actorName) }
    }

    private fun notifyMembers(group: MoneyGroup, recipients: Set<Long>, buttonKey: String, text: (String?) -> String) {
        recipients.forEach { memberId ->
            val language = userInfoService.getUserInfoByTelegramId(memberId)?.language
            runCatching { execute(notice(memberId, language, buttonKey, text(language))) }
                .onFailure { log.warn("Failed to notify member {} of group {}: {}", memberId, group.id, it.message) }
        }
    }

    private fun notice(memberId: Long, language: String?, buttonKey: String, text: String): SendMessage =
        SendMessage(memberId.toString(), text).apply {
            if (miniAppGateway.enabled) {
                replyMarkup = miniAppGateway.keyboard(buttonKey, language, MiniAppScreen.CATEGORIES)
            }
        }
}
