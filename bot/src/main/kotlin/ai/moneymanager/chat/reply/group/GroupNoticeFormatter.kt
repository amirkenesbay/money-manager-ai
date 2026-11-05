package ai.moneymanager.chat.reply.group

import ai.moneymanager.chat.reply.common.LINE_BREAK
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.event.GroupSettingChange
import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.LocalizationService
import org.springframework.stereotype.Component

private const val MAX_OPERATION_LINES = 10
private const val DETAIL_SEPARATOR = ", "
private const val DESCRIPTION_PREFIX = " · "
private const val CURRENCY_LABEL_TEMPLATE = "%s %s"

enum class NoticeTopic(val emoji: String) {
    OPERATIONS("📝"),
    CATEGORIES("🏷"),
    GROUP("⚙️"),
    MEMBERS("👋"),
    DELETED("🗑"),
}

@Component
class GroupNoticeFormatter(
    private val localizationService: LocalizationService,
) {
    fun notice(topic: NoticeTopic, groupName: String, actorName: String, body: String, language: String?): String =
        t("group.notice.header", language, topic.emoji, escapeHtml(groupName), escapeHtml(actorName)) + LINE_BREAK + body

    fun categoryCreated(icon: String?, name: String, language: String?): String =
        t("group.notice.category_created", language, categoryLabel(icon, name))

    fun categoryUpdated(oldIcon: String?, oldName: String, newIcon: String?, newName: String, language: String?): String =
        t("group.notice.category_updated", language, categoryLabel(oldIcon, oldName), categoryLabel(newIcon, newName))

    fun categoryDeleted(icon: String?, name: String, language: String?): String =
        t("group.notice.category_deleted", language, categoryLabel(icon, name))

    fun setting(change: GroupSettingChange, currency: Currency, language: String?): String = when (change) {
        is GroupSettingChange.Renamed -> t("group.notice.renamed", language, escapeHtml(change.oldName))
        is GroupSettingChange.CurrencyChanged ->
            t("group.notice.currency", language, currencyLabel(change.oldCurrency), currencyLabel(change.newCurrency))
        is GroupSettingChange.InitialBalanceChanged ->
            t("group.notice.initial_balance", language, formatAmount(change.oldAmount, currency), formatAmount(change.newAmount, currency))
        is GroupSettingChange.MonthStartChanged -> t("group.notice.month_start", language, change.newDay, change.oldDay)
    }

    fun groupDeleted(language: String?): String = t("group.notice.deleted", language)

    fun memberJoined(language: String?): String = t("group.notice.joined", language)

    fun operations(changes: List<OperationChange>, currency: Currency, language: String?): String {
        val lines = changes.take(MAX_OPERATION_LINES).map { operationLine(it, currency, language) }
        val more = (changes.size - MAX_OPERATION_LINES).takeIf { it > 0 }?.let { t("group.notice.operation.more", language, it) }
        return (lines + listOfNotNull(more)).joinToString(LINE_BREAK)
    }

    private fun operationLine(change: OperationChange, currency: Currency, language: String?): String = when (change) {
        is OperationChange.Added -> t("group.notice.operation.added", language, operationLabel(change.operation), amount(change.operation, currency), description(change.operation))
        is OperationChange.Deleted -> t("group.notice.operation.deleted", language, operationLabel(change.operation), amount(change.operation, currency), description(change.operation))
        is OperationChange.Edited -> t(
            "group.notice.operation.edited", language,
            operationLabel(change.after), amount(change.after, currency), description(change.after), previous(change, currency, language),
        )
    }

    private fun previous(change: OperationChange.Edited, currency: Currency, language: String?): String {
        val before = change.before
        val after = change.after
        return listOfNotNull(
            operationLabel(before).takeIf { before.categoryId != after.categoryId },
            amount(before, currency).takeIf { before.amount.compareTo(after.amount) != 0 || before.type != after.type },
            before.operationDate.format(shortDateFormatter).takeIf { before.operationDate != after.operationDate },
            (before.description?.let(::escapeHtml) ?: t("group.notice.operation.no_description", language)).takeIf { before.description != after.description },
        ).joinToString(DETAIL_SEPARATOR)
    }

    private fun operationLabel(operation: FinanceOperationEntity): String = categoryLabel(operation.categoryIcon, operation.categoryName)

    private fun amount(operation: FinanceOperationEntity, currency: Currency): String = formatSignedAmount(operation.type, operation.amount, currency)

    private fun description(operation: FinanceOperationEntity): String =
        operation.description?.takeIf { it.isNotBlank() }?.let { DESCRIPTION_PREFIX + escapeHtml(it) }.orEmpty()

    private fun categoryLabel(icon: String?, name: String): String = escapeHtml(formatIconPrefix(icon) + name)

    private fun currencyLabel(currency: Currency): String = CURRENCY_LABEL_TEMPLATE.format(currency.name, currency.symbol)

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t(key, language, *args)
}
