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
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

private const val RECENT_OPERATIONS_LIMIT = 5
private const val RECENT_OPERATIONS_HEADER = "🕒 Последние:"

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeManagementReply(
    financeHistoryService: FinanceHistoryService
) {
    reply {
        state = MoneyManagerState.FINANCE_MANAGEMENT

        message {
            val details = context.buildFinanceDetails()
            val recentSection = context.buildRecentOperationsSection(financeHistoryService)

            text = """
                |📊 Финансы
                |
                |Управляй расходами и доходами${if (details.isNotEmpty()) "\n$details" else ""}$recentSection
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "📉 Расход"
                        type = MoneyManagerButtonType.FINANCE_ADD_EXPENSE
                    }
                    button {
                        text = "📈 Доход"
                        type = MoneyManagerButtonType.FINANCE_ADD_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = "📋 История"
                        type = MoneyManagerButtonType.FINANCE_HISTORY
                    }
                    button {
                        text = "📈 Отчёт"
                        type = MoneyManagerButtonType.FINANCE_REPORT
                    }
                }
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

private fun MoneyManagerContext.buildFinanceDetails(): String = buildString {
    selectedCategory?.let {
        val icon = it.icon ?: DEFAULT_CATEGORY_ICON
        append("\n$icon Категория: ${it.name}")
    }
    financeAmount?.let { append("\n💰 Сумма: $it") }
    financeComment?.let { append("\n💬 Комментарий: $it") }
    selectedDate?.let { append("\n📅 Дата: $it") }
}

private fun MoneyManagerContext.buildRecentOperationsSection(
    financeHistoryService: FinanceHistoryService
): String {
    val groupId = userInfo?.activeGroupId ?: return ""
    val operations = financeHistoryService.getRecentOperations(groupId, RECENT_OPERATIONS_LIMIT)
    if (operations.isEmpty()) return ""

    return buildString {
        append(SECTION_SEPARATOR_WITH_BLANK_LINE)
        append("\n$RECENT_OPERATIONS_HEADER")
        operations.forEach { append("\n${formatRecentOperation(it)}") }
    }
}

private fun formatRecentOperation(operation: FinanceOperationEntity): String {
    val date = operation.operationDate.format(shortDateFormatter)
    val icon = operation.categoryIcon ?: DEFAULT_CATEGORY_ICON
    return "$date $icon ${operation.categoryName} ${formatSignedAmount(operation.type, operation.amount)}"
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeOperationSavedReply() {
    reply {
        state = MoneyManagerState.FINANCE_OPERATION_SAVED

        message {
            val typeLabel = when (context.financeOperationType) {
                CategoryType.INCOME -> "Доход"
                else -> "Расход"
            }

            text = """
                |✅ $typeLabel добавлен!
                |${context.buildFinanceDetails()}
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "📉 Расход"
                        type = MoneyManagerButtonType.FINANCE_ADD_EXPENSE
                    }
                    button {
                        text = "📈 Доход"
                        type = MoneyManagerButtonType.FINANCE_ADD_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}
