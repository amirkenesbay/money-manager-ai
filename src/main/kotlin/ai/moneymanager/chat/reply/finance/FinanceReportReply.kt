package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportMenuReply() {
    reply {
        state = MoneyManagerState.FINANCE_REPORT_MENU

        message {
            text = """
                |📈 Отчёты
                |
                |Выбери тип отчёта:
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "📊 Сравнение месяцев"
                        type = MoneyManagerButtonType.REPORT_COMPARISON
                    }
                }
                buttonRow {
                    button {
                        text = "📈 Аналитика"
                        type = MoneyManagerButtonType.REPORT_ANALYTICS
                    }
                }
                buttonRow {
                    button {
                        text = "👥 По участникам"
                        type = MoneyManagerButtonType.REPORT_BY_MEMBERS
                    }
                }
                buttonRow {
                    button {
                        text = "📂 По категории"
                        type = MoneyManagerButtonType.REPORT_BY_CATEGORY
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportComparisonReply() {
    reportWithNavigationReply(MoneyManagerState.FINANCE_REPORT_COMPARISON)
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportAnalyticsReply() {
    reportWithNavigationReply(MoneyManagerState.FINANCE_REPORT_ANALYTICS)
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportMembersReply() {
    reportWithNavigationReply(MoneyManagerState.FINANCE_REPORT_MEMBERS)
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportCategorySelectReply() {
    reply {
        state = MoneyManagerState.FINANCE_REPORT_CATEGORY_SELECT

        message {
            val categories = context.categories

            text = """
                |📂 Отчёт по категории
                |
                |Выбери категорию:
            """.trimMargin()

            keyboard {
                categories.forEach { category ->
                    val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                    buttonRow {
                        button {
                            text = "$icon ${category.name}"
                            type = MoneyManagerButtonType.REPORT_CATEGORY_ITEM
                        }
                    }
                }
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_REPORT_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportCategoryViewReply() {
    reply {
        state = MoneyManagerState.FINANCE_REPORT_CATEGORY_VIEW

        message {
            text = context.reportText ?: "Загрузка..."

            keyboard {
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_REPORT_MENU
                    }
                }
            }
        }
    }
}

private fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.reportWithNavigationReply(
    reportState: MoneyManagerState
) {
    reply {
        state = reportState

        message {
            text = context.reportText ?: "Загрузка..."

            keyboard {
                buttonRow {
                    button {
                        text = "◀ Пред."
                        type = MoneyManagerButtonType.REPORT_PREV
                    }
                    button {
                        text = "▶ След."
                        type = MoneyManagerButtonType.REPORT_NEXT
                    }
                }
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_REPORT_MENU
                    }
                }
            }
        }
    }
}