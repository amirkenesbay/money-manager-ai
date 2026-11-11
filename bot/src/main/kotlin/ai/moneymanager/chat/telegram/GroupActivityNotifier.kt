package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.chat.reply.group.GroupNoticeFormatter
import ai.moneymanager.chat.reply.group.NoticeTopic
import ai.moneymanager.chat.telegram.widget.NoticeCardRenderer
import ai.moneymanager.chat.telegram.widget.WidgetSender
import ai.moneymanager.domain.event.CategoryCreated
import ai.moneymanager.domain.event.CategoryDeleted
import ai.moneymanager.domain.event.CategoryUpdated
import ai.moneymanager.domain.event.GroupDeleted
import ai.moneymanager.domain.event.GroupSettingChanged
import ai.moneymanager.domain.event.MemberJoined
import ai.moneymanager.domain.event.OperationChanged
import ai.moneymanager.domain.event.OperationsChanged
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.GroupNoticeLevel
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.BudgetAlert
import ai.moneymanager.service.BudgetAlertService
import ai.moneymanager.service.BudgetService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

private const val OPEN_CATEGORIES_BUTTON_KEY = "mini_app.button.categories"
private const val OPEN_BUDGETS_BUTTON_KEY = "mini_app.button.budgets"
private const val OPEN_OPERATIONS_BUTTON_KEY = "mini_app.button.operations"
private const val OPEN_GROUPS_BUTTON_KEY = "mini_app.button.groups"
private const val BUDGET_NEAR_KEY = "budget.notice.near"
private const val BUDGET_OVER_KEY = "budget.notice.over"
private const val OPERATION_FLUSH_INTERVAL_MS = 20_000L

private data class NoticeButton(val textKey: String, val screen: MiniAppScreen)

private val CATEGORIES_BUTTON = NoticeButton(OPEN_CATEGORIES_BUTTON_KEY, MiniAppScreen.CATEGORIES)
private val BUDGETS_BUTTON = NoticeButton(OPEN_BUDGETS_BUTTON_KEY, MiniAppScreen.CATEGORIES)
private val OPERATIONS_BUTTON = NoticeButton(OPEN_OPERATIONS_BUTTON_KEY, MiniAppScreen.OPERATIONS)
private val GROUPS_BUTTON = NoticeButton(OPEN_GROUPS_BUTTON_KEY, MiniAppScreen.GROUPS)

private val IMPORTANT_LEVELS = setOf(GroupNoticeLevel.ALL, GroupNoticeLevel.IMPORTANT)
private val OPERATION_LEVELS = setOf(GroupNoticeLevel.ALL)

@Service
class GroupActivityNotifier(
    private val groupService: GroupService,
    private val budgetAlertService: BudgetAlertService,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
    private val miniAppGateway: MiniAppGateway,
    private val noticeFormatter: GroupNoticeFormatter,
    private val operationBuffer: OperationNoticeBuffer,
    private val noticeCardRenderer: NoticeCardRenderer,
    private val budgetService: BudgetService,
    private val widgetSender: WidgetSender,
    @Value("\${chat-machinist.bot.token}")
    botToken: String,
) : DefaultAbsSender(DefaultBotOptions(), botToken) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    fun onCategoryCreated(event: CategoryCreated) {
        val card = { group: MoneyGroup, actorName: String, language: String? -> noticeCardRenderer.categoryCreated(actorName, group, event.icon, event.name, language) }
        notifyOthers(event.groupId, event.actorId, NoticeTopic.CATEGORIES, CATEGORIES_BUTTON, card = card) { _, language ->
            noticeFormatter.categoryCreated(event.icon, event.name, language)
        }
    }

    @EventListener
    fun onCategoryUpdated(event: CategoryUpdated) {
        notifyOthers(event.groupId, event.actorId, NoticeTopic.CATEGORIES, CATEGORIES_BUTTON) { _, language ->
            noticeFormatter.categoryUpdated(event.oldIcon, event.oldName, event.newIcon, event.newName, language)
        }
    }

    @EventListener
    fun onCategoryDeleted(event: CategoryDeleted) {
        notifyOthers(event.groupId, event.actorId, NoticeTopic.CATEGORIES, CATEGORIES_BUTTON) { _, language ->
            noticeFormatter.categoryDeleted(event.icon, event.name, language)
        }
    }

    @EventListener
    fun onGroupSettingChanged(event: GroupSettingChanged) {
        notifyOthers(event.groupId, event.actorId, NoticeTopic.GROUP, GROUPS_BUTTON) { group, language ->
            noticeFormatter.setting(event.change, group.currency, language)
        }
    }

    @EventListener
    fun onMemberJoined(event: MemberJoined) {
        notifyOthers(event.groupId, event.memberId, NoticeTopic.MEMBERS, GROUPS_BUTTON) { _, language ->
            noticeFormatter.memberJoined(language)
        }
    }

    @EventListener
    fun onGroupDeleted(event: GroupDeleted) {
        val actorName = actorName(event.actorId)
        recipients(event.groupId, event.memberIds - event.actorId, IMPORTANT_LEVELS).forEach { member ->
            val text = noticeFormatter.notice(NoticeTopic.DELETED, event.groupName, actorName, noticeFormatter.groupDeleted(member.language), member.language)
            send(member, event.groupId, text, null)
        }
    }

    @EventListener
    fun onOperationChanged(event: OperationChanged) {
        operationBuffer.add(event)
    }

    @Scheduled(fixedDelay = OPERATION_FLUSH_INTERVAL_MS)
    fun flushOperationNotices() {
        operationBuffer.drainReady().forEach { batch ->
            val card = { group: MoneyGroup, actorName: String, language: String? -> noticeCardRenderer.added(batch.changes, actorName, group, budgetService.budgets(group), language) }
            notifyOthers(batch.groupId, batch.actorId, NoticeTopic.OPERATIONS, OPERATIONS_BUTTON, OPERATION_LEVELS, card) { group, language ->
                noticeFormatter.operations(batch.changes, group.currency, language)
            }
        }
    }

    @EventListener
    fun onOperationsChanged(event: OperationsChanged) {
        runCatching {
            val group = groupService.getGroup(event.groupId) ?: return
            budgetAlertService.claimNewAlerts(group).forEach { alert ->
                recipients(event.groupId, group.memberIds, IMPORTANT_LEVELS).forEach { member ->
                    val card = noticeCardRenderer.budget(alert.budget, alert.threshold, group, member.language)
                    send(member, event.groupId, budgetNotice(alert, group, member.language), BUDGETS_BUTTON, parseHtml = false, card = card)
                }
            }
        }.onFailure { log.warn("Failed to check budgets of group {}: {}", event.groupId, it.message) }
    }

    private fun budgetNotice(alert: BudgetAlert, group: MoneyGroup, language: String?): String {
        val budget = alert.budget
        val category = formatIconPrefix(budget.category.icon) + budget.category.name
        val spent = formatAmount(budget.spent, group.currency)
        val limit = formatAmount(budget.effectiveLimit, group.currency)
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

    private fun notifyOthers(
        groupId: ObjectId,
        actorId: Long,
        topic: NoticeTopic,
        button: NoticeButton,
        levels: Set<GroupNoticeLevel> = IMPORTANT_LEVELS,
        card: ((MoneyGroup, String, String?) -> String?)? = null,
        body: (MoneyGroup, String?) -> String,
    ) {
        runCatching {
            val group = groupService.getGroup(groupId) ?: return
            val actorName = actorName(actorId)
            recipients(groupId, group.memberIds - actorId, levels).forEach { member ->
                val text = noticeFormatter.notice(topic, group.name, actorName, body(group, member.language), member.language)
                send(member, groupId, text, button, card = card?.invoke(group, actorName, member.language))
            }
        }.onFailure { log.warn("Failed to notify group {} about {}: {}", groupId, topic, it.message) }
    }

    private fun recipients(groupId: ObjectId, memberIds: Set<Long>, levels: Set<GroupNoticeLevel>): List<UserInfo> =
        memberIds.mapNotNull { userInfoService.getUserInfoByTelegramId(it) }.filter { it.noticeLevel(groupId) in levels }

    private fun actorName(actorId: Long): String = userInfoService.displayNames(setOf(actorId)).getValue(actorId)

    private fun send(member: UserInfo, groupId: ObjectId, text: String, button: NoticeButton?, parseHtml: Boolean = true, card: String? = null) {
        val chatId = checkNotNull(member.telegramUserId).toString()
        val message = SendMessage(chatId, text).apply {
            if (parseHtml) parseMode = ParseMode.HTML
            if (button != null && miniAppGateway.enabled) replyMarkup = miniAppGateway.keyboard(button.textKey, member.language, button.screen)
        }
        runCatching { if (card == null) execute(message) else widgetSender.sendCard(message, card) { execute(message).messageId } }
            .onFailure { log.warn("Failed to notify member {} of group {}: {}", chatId, groupId, it.message) }
    }
}
