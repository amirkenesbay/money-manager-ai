package ai.moneymanager.chat.reply.ai

import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.domain.text.formatAmount
import ai.moneymanager.chat.reply.common.formatDescriptionSuffix
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.formatTime
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.AiPendingAction.CategoryAction
import ai.moneymanager.domain.model.nlp.AiPendingAction.GroupAction
import ai.moneymanager.domain.model.nlp.AiPendingAction.NotificationAction
import ai.moneymanager.domain.model.nlp.AiPendingAction.RecentOperationAction
import ai.moneymanager.domain.model.nlp.AiPendingAction.TransactionAction
import ai.moneymanager.service.LocalizationService
import java.math.BigDecimal

private const val EDIT_CHANGES_SEPARATOR = ", "
private const val MISSING_ICON = "—"
private const val CONFIRM_ADD_EXPENSE_KEY = "ai.confirm.transaction.add.expense"
private const val CONFIRM_ADD_INCOME_KEY = "ai.confirm.transaction.add.income"
private const val CONFIRM_ADD_WITH_NEW_CATEGORY_EXPENSE_KEY = "ai.confirm.transaction.add_with_new_category.expense"
private const val CONFIRM_ADD_WITH_NEW_CATEGORY_INCOME_KEY = "ai.confirm.transaction.add_with_new_category.income"
private const val BATCH_ITEM_EXPENSE_KEY = "ai.confirm.batch.item.expense"
private const val BATCH_ITEM_INCOME_KEY = "ai.confirm.batch.item.income"
private const val BATCH_ITEM_NEW_CATEGORY_KEY = "ai.confirm.batch.item.new_category"

fun AiPendingAction.describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
    when (this) {
        is CategoryAction.Create -> localizationService.t(
            "ai.confirm.category.create", language, iconPart(icon), name, typeLabel(localizationService, language, type)
        )
        is CategoryAction.Delete -> localizationService.t(
            "ai.confirm.category.delete", language, iconPart(category.icon), category.name,
            typeLabel(localizationService, language, category.type)
        )
        is CategoryAction.Rename -> localizationService.t(
            "ai.confirm.category.rename", language, category.name, newName, typeLabel(localizationService, language, category.type)
        )
        is CategoryAction.ChangeIcon -> localizationService.t(
            "ai.confirm.category.change_icon", language, category.name, category.icon ?: MISSING_ICON, newIcon
        )
        is CategoryAction.DeleteAll -> localizationService.t("ai.confirm.category.delete_all", language, count)
        is GroupAction.Create -> localizationService.t("ai.confirm.group.create", language, name)
        is GroupAction.Delete -> localizationService.t("ai.confirm.group.delete", language, group.name, group.memberIds.size)
        is NotificationAction.CreateDaily -> localizationService.t(
            "ai.confirm.notification.create_daily", language, name, formatTime(hour, minute)
        )
        is NotificationAction.Delete -> localizationService.t(
            "ai.confirm.notification.delete", language, formatIconPrefix(notification.icon), notification.name
        )
        is RecentOperationAction.Delete -> localizationService.t(
            "ai.confirm.operation.delete", language, formatIconPrefix(operation.categoryIcon), operation.categoryName,
            formatSignedAmount(operation.type, operation.amount, currency)
        )
        is RecentOperationAction.Edit -> describeEdit(localizationService, currency, language)
        is TransactionAction -> render(localizationService, currency, language, confirmKey())
    }

fun TransactionAction.describeBatchItem(localizationService: LocalizationService, currency: Currency, language: String?): String {
    val key = if (type == CategoryType.EXPENSE) BATCH_ITEM_EXPENSE_KEY else BATCH_ITEM_INCOME_KEY
    val base = escapeHtml(render(localizationService, currency, language, key))
    if (!isNewCategory) return base
    return base + localizationService.t(BATCH_ITEM_NEW_CATEGORY_KEY, language)
}

private fun RecentOperationAction.Edit.describeEdit(
    localizationService: LocalizationService,
    currency: Currency,
    language: String?,
): String {
    val changes = listOfNotNull(
        newAmount?.let { localizationService.t("ai.confirm.operation.edit.amount", language, formatAmount(it, currency)) },
        newCategory?.let { localizationService.t("ai.confirm.operation.edit.category", language, formatIconPrefix(it.icon), it.name) },
        newOperationDate?.let { localizationService.t("ai.confirm.operation.edit.date", language, it) }
    ).joinToString(EDIT_CHANGES_SEPARATOR)
    return localizationService.t(
        "ai.confirm.operation.edit",
        language,
        formatIconPrefix(operation.categoryIcon),
        operation.categoryName,
        formatSignedAmount(operation.type, operation.amount, currency),
        changes
    )
}

private fun TransactionAction.confirmKey(): String = when {
    isNewCategory && type == CategoryType.EXPENSE -> CONFIRM_ADD_WITH_NEW_CATEGORY_EXPENSE_KEY
    isNewCategory -> CONFIRM_ADD_WITH_NEW_CATEGORY_INCOME_KEY
    type == CategoryType.EXPENSE -> CONFIRM_ADD_EXPENSE_KEY
    else -> CONFIRM_ADD_INCOME_KEY
}

private fun TransactionAction.render(
    localizationService: LocalizationService,
    currency: Currency,
    language: String?,
    key: String,
): String = localizationService.t(
    key,
    language,
    formatIconPrefix(categoryDisplayIcon),
    categoryDisplayName,
    formatAmount(BigDecimal.valueOf(amount), currency),
    formatDescriptionSuffix(description)
)

private fun iconPart(icon: String?): String = if (icon != null) "$icon " else ""

private fun typeLabel(localizationService: LocalizationService, language: String?, type: CategoryType): String = when (type) {
    CategoryType.EXPENSE -> localizationService.t("ai.type.expense", language)
    CategoryType.INCOME -> localizationService.t("ai.type.income", language)
}
