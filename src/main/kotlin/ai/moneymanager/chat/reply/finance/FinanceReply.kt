package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.SECTION_SEPARATOR_WITH_BLANK_LINE
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.shortDateFormatter
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

private const val RECENT_OPERATIONS_LIMIT = 5

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeManagementReply(
    financeHistoryService: FinanceHistoryService,
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_MANAGEMENT

        message {
            val lang = context.userInfo?.language
            val title = localizationService.t("finance.management.title", lang)
            val body = localizationService.t("finance.management.body", lang)
            val details = context.buildFinanceDetails(localizationService, lang)
            val recentSection = context.buildRecentOperationsSection(financeHistoryService, localizationService, lang)

            text = """
                |$title
                |
                |$body${if (details.isNotEmpty()) "\n$details" else ""}$recentSection
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.button.add_expense", lang)
                        type = MoneyManagerButtonType.FINANCE_ADD_EXPENSE
                    }
                    button {
                        text = localizationService.t("finance.button.add_income", lang)
                        type = MoneyManagerButtonType.FINANCE_ADD_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.button.history", lang)
                        type = MoneyManagerButtonType.FINANCE_HISTORY
                    }
                    button {
                        text = localizationService.t("finance.button.report", lang)
                        type = MoneyManagerButtonType.FINANCE_REPORT
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

private fun MoneyManagerContext.buildFinanceDetails(
    localizationService: LocalizationService,
    lang: String?
): String = buildString {
    selectedCategory?.let {
        val icon = it.icon ?: DEFAULT_CATEGORY_ICON
        append(localizationService.t("finance.management.detail.category", lang, icon, it.name))
    }
    financeAmount?.let { append(localizationService.t("finance.management.detail.amount", lang, it)) }
    financeComment?.let { append(localizationService.t("finance.management.detail.comment", lang, it)) }
    selectedDate?.let { append(localizationService.t("finance.management.detail.date", lang, it)) }
}

private fun MoneyManagerContext.buildRecentOperationsSection(
    financeHistoryService: FinanceHistoryService,
    localizationService: LocalizationService,
    lang: String?
): String {
    val groupId = userInfo?.activeGroupId ?: return ""
    val operations = financeHistoryService.getRecentOperations(groupId, RECENT_OPERATIONS_LIMIT)
    if (operations.isEmpty()) return ""

    val header = localizationService.t("finance.management.recent_header", lang)
    return buildString {
        append(SECTION_SEPARATOR_WITH_BLANK_LINE)
        append("\n$header")
        operations.forEach { append("\n${formatRecentOperation(it)}") }
    }
}

private fun formatRecentOperation(operation: FinanceOperationEntity): String {
    val date = operation.operationDate.format(shortDateFormatter)
    val icon = operation.categoryIcon ?: DEFAULT_CATEGORY_ICON
    return "$date $icon ${operation.categoryName} ${formatSignedAmount(operation.type, operation.amount)}"
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeOperationSavedReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_OPERATION_SAVED

        message {
            val lang = context.userInfo?.language
            val titleKey = when (context.financeOperationType) {
                CategoryType.INCOME -> "finance.saved.title.income"
                else -> "finance.saved.title.expense"
            }
            val title = localizationService.t(titleKey, lang)
            val details = context.buildFinanceDetails(localizationService, lang)

            text = """
                |$title
                |$details
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.button.add_expense", lang)
                        type = MoneyManagerButtonType.FINANCE_ADD_EXPENSE
                    }
                    button {
                        text = localizationService.t("finance.button.add_income", lang)
                        type = MoneyManagerButtonType.FINANCE_ADD_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}
