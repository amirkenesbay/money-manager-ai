package ai.moneymanager.domain.model.nlp

import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.reply.common.formatDescriptionSuffix
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.formatTime
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.service.LocalizationService
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

sealed class AiPendingAction {
    abstract fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String

    sealed class CategoryAction : AiPendingAction() {
        data class Create(
            val name: String,
            val type: CategoryType,
            val icon: String?
        ) : CategoryAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String {
                val iconPart = if (icon != null) "$icon " else ""
                return localizationService.t(
                    "ai.confirm.category.create",
                    language,
                    iconPart,
                    name,
                    typeLabel(localizationService, language, type)
                )
            }
        }

        data class Delete(
            val category: Category
        ) : CategoryAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String {
                val iconPart = if (category.icon != null) "${category.icon} " else ""
                return localizationService.t(
                    "ai.confirm.category.delete",
                    language,
                    iconPart,
                    category.name,
                    typeLabel(localizationService, language, category.type)
                )
            }
        }

        data class Rename(
            val category: Category,
            val newName: String
        ) : CategoryAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
                localizationService.t(
                    "ai.confirm.category.rename",
                    language,
                    category.name,
                    newName,
                    typeLabel(localizationService, language, category.type)
                )
        }

        data class ChangeIcon(
            val category: Category,
            val newIcon: String
        ) : CategoryAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String {
                val oldIcon = category.icon ?: "—"
                return localizationService.t(
                    "ai.confirm.category.change_icon",
                    language,
                    category.name,
                    oldIcon,
                    newIcon
                )
            }
        }

        data class DeleteAll(
            val groupId: ObjectId,
            val count: Int
        ) : CategoryAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
                localizationService.t("ai.confirm.category.delete_all", language, count)
        }
    }

    sealed class GroupAction : AiPendingAction() {
        data class Create(
            val name: String
        ) : GroupAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
                localizationService.t("ai.confirm.group.create", language, name)
        }

        data class Delete(
            val group: MoneyGroup
        ) : GroupAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
                localizationService.t("ai.confirm.group.delete", language, group.name, group.memberIds.size)
        }
    }

    sealed class NotificationAction : AiPendingAction() {
        data class CreateDaily(
            val name: String,
            val hour: Int,
            val minute: Int
        ) : NotificationAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
                localizationService.t(
                    "ai.confirm.notification.create_daily",
                    language,
                    name,
                    formatTime(hour, minute)
                )
        }

        data class Delete(
            val notification: NotificationEntity
        ) : NotificationAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
                localizationService.t(
                    "ai.confirm.notification.delete",
                    language,
                    formatIconPrefix(notification.icon),
                    notification.name
                )
        }
    }

    sealed class RecentOperationAction : AiPendingAction() {
        data class Delete(
            val operation: FinanceOperationEntity
        ) : RecentOperationAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String =
                localizationService.t(
                    "ai.confirm.operation.delete",
                    language,
                    formatIconPrefix(operation.categoryIcon),
                    operation.categoryName,
                    formatSignedAmount(operation.type, operation.amount, currency)
                )
        }

        data class Edit(
            val operation: FinanceOperationEntity,
            val newAmount: BigDecimal?,
            val newCategory: Category?,
            val newOperationDate: LocalDate?
        ) : RecentOperationAction() {
            override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String {
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
        }

        companion object {
            private const val EDIT_CHANGES_SEPARATOR = ", "
        }
    }

    sealed class TransactionAction : AiPendingAction() {
        abstract val groupId: ObjectId
        abstract val creatorId: Long
        abstract val type: CategoryType
        abstract val amount: Double
        abstract val description: String?
        abstract val operationDate: LocalDate
        abstract val categoryDisplayName: String
        abstract val categoryDisplayIcon: String?
        abstract val isNewCategory: Boolean

        override fun describe(localizationService: LocalizationService, currency: Currency, language: String?): String {
            val key = when {
                isNewCategory && type == CategoryType.EXPENSE -> CONFIRM_ADD_WITH_NEW_CATEGORY_EXPENSE_KEY
                isNewCategory -> CONFIRM_ADD_WITH_NEW_CATEGORY_INCOME_KEY
                type == CategoryType.EXPENSE -> CONFIRM_ADD_EXPENSE_KEY
                else -> CONFIRM_ADD_INCOME_KEY
            }
            return render(localizationService, currency, language, key)
        }

        fun describeBatchItem(localizationService: LocalizationService, currency: Currency, language: String?): String {
            val key = if (type == CategoryType.EXPENSE) BATCH_ITEM_EXPENSE_KEY else BATCH_ITEM_INCOME_KEY
            val base = escapeHtml(render(localizationService, currency, language, key))
            if (!isNewCategory) return base
            return base + localizationService.t(BATCH_ITEM_NEW_CATEGORY_KEY, language)
        }

        private fun render(localizationService: LocalizationService, currency: Currency, language: String?, key: String): String =
            localizationService.t(
                key,
                language,
                formatIconPrefix(categoryDisplayIcon),
                categoryDisplayName,
                formatAmount(BigDecimal.valueOf(amount), currency),
                formatDescriptionSuffix(description)
            )

        data class Add(
            override val groupId: ObjectId,
            override val creatorId: Long,
            override val type: CategoryType,
            override val amount: Double,
            val category: Category,
            override val description: String?,
            override val operationDate: LocalDate
        ) : TransactionAction() {
            override val categoryDisplayName: String get() = category.name
            override val categoryDisplayIcon: String? get() = category.icon
            override val isNewCategory: Boolean get() = false
        }

        data class AddWithNewCategory(
            override val groupId: ObjectId,
            override val creatorId: Long,
            override val type: CategoryType,
            override val amount: Double,
            val suggestedCategoryName: String,
            val suggestedCategoryIcon: String,
            override val description: String?,
            override val operationDate: LocalDate
        ) : TransactionAction() {
            override val categoryDisplayName: String get() = suggestedCategoryName
            override val categoryDisplayIcon: String get() = suggestedCategoryIcon
            override val isNewCategory: Boolean get() = true
        }

        companion object {
            private const val CONFIRM_ADD_EXPENSE_KEY = "ai.confirm.transaction.add.expense"
            private const val CONFIRM_ADD_INCOME_KEY = "ai.confirm.transaction.add.income"
            private const val CONFIRM_ADD_WITH_NEW_CATEGORY_EXPENSE_KEY = "ai.confirm.transaction.add_with_new_category.expense"
            private const val CONFIRM_ADD_WITH_NEW_CATEGORY_INCOME_KEY = "ai.confirm.transaction.add_with_new_category.income"
            private const val BATCH_ITEM_EXPENSE_KEY = "ai.confirm.batch.item.expense"
            private const val BATCH_ITEM_INCOME_KEY = "ai.confirm.batch.item.income"
            private const val BATCH_ITEM_NEW_CATEGORY_KEY = "ai.confirm.batch.item.new_category"
        }
    }
}

private fun typeLabel(
    localizationService: LocalizationService,
    language: String?,
    type: CategoryType
): String = when (type) {
    CategoryType.EXPENSE -> localizationService.t("ai.type.expense", language)
    CategoryType.INCOME -> localizationService.t("ai.type.income", language)
}
