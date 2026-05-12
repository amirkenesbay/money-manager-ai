package ai.moneymanager.domain.model.nlp

import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.reply.common.formatDescriptionSuffix
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.service.LocalizationService
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

sealed class AiPendingAction {
    abstract fun describe(localizationService: LocalizationService, language: String?): String

    sealed class CategoryAction : AiPendingAction() {
        data class Create(
            val name: String,
            val type: CategoryType,
            val icon: String?
        ) : CategoryAction() {
            override fun describe(localizationService: LocalizationService, language: String?): String {
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
            override fun describe(localizationService: LocalizationService, language: String?): String {
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
            override fun describe(localizationService: LocalizationService, language: String?): String =
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
            override fun describe(localizationService: LocalizationService, language: String?): String {
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
            override fun describe(localizationService: LocalizationService, language: String?): String =
                localizationService.t("ai.confirm.category.delete_all", language, count)
        }
    }

    sealed class TransactionAction : AiPendingAction() {
        data class Add(
            val groupId: ObjectId,
            val creatorId: Long,
            val type: CategoryType,
            val amount: Double,
            val category: Category,
            val description: String?,
            val operationDate: LocalDate
        ) : TransactionAction() {
            override fun describe(localizationService: LocalizationService, language: String?): String =
                renderTransactionConfirmation(
                    localizationService, language, type, amount, description,
                    categoryName = category.name,
                    icon = category.icon,
                    expenseKey = CONFIRM_ADD_EXPENSE_KEY,
                    incomeKey = CONFIRM_ADD_INCOME_KEY
                )
        }

        data class AddWithNewCategory(
            val groupId: ObjectId,
            val creatorId: Long,
            val type: CategoryType,
            val amount: Double,
            val suggestedCategoryName: String,
            val suggestedCategoryIcon: String,
            val description: String?,
            val operationDate: LocalDate
        ) : TransactionAction() {
            override fun describe(localizationService: LocalizationService, language: String?): String =
                renderTransactionConfirmation(
                    localizationService, language, type, amount, description,
                    categoryName = suggestedCategoryName,
                    icon = suggestedCategoryIcon,
                    expenseKey = CONFIRM_ADD_WITH_NEW_CATEGORY_EXPENSE_KEY,
                    incomeKey = CONFIRM_ADD_WITH_NEW_CATEGORY_INCOME_KEY
                )
        }

        companion object {
            private const val CONFIRM_ADD_EXPENSE_KEY = "ai.confirm.transaction.add.expense"
            private const val CONFIRM_ADD_INCOME_KEY = "ai.confirm.transaction.add.income"
            private const val CONFIRM_ADD_WITH_NEW_CATEGORY_EXPENSE_KEY = "ai.confirm.transaction.add_with_new_category.expense"
            private const val CONFIRM_ADD_WITH_NEW_CATEGORY_INCOME_KEY = "ai.confirm.transaction.add_with_new_category.income"

            private fun renderTransactionConfirmation(
                localizationService: LocalizationService,
                language: String?,
                type: CategoryType,
                amount: Double,
                description: String?,
                categoryName: String,
                icon: String?,
                expenseKey: String,
                incomeKey: String
            ): String {
                val key = if (type == CategoryType.EXPENSE) expenseKey else incomeKey
                return localizationService.t(
                    key,
                    language,
                    formatIconPrefix(icon),
                    categoryName,
                    formatAmount(BigDecimal.valueOf(amount)),
                    formatDescriptionSuffix(description)
                )
            }
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
