package ai.moneymanager.chat.reply.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistoryReply() {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_VIEW

        message {
            text = context.historyReport ?: "Загрузка..."

            keyboard {
                buttonRow {
                    button {
                        text = "📅 Изменить период"
                        type = MoneyManagerButtonType.CHANGE_PERIOD
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectStartDateReply() {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE

        message {
            text = "📅 Выберите начало периода"

            keyboard {
                buttonRow {
                    button {
                        text = "Этот месяц"
                        type = MoneyManagerButtonType.QUICK_PERIOD_THIS_MONTH
                    }
                    button {
                        text = "Прошлый месяц"
                        type = MoneyManagerButtonType.QUICK_PERIOD_LAST_MONTH
                    }
                }
                buttonRow {
                    button {
                        text = "Этот год"
                        type = MoneyManagerButtonType.QUICK_PERIOD_THIS_YEAR
                    }
                }
                buttonRow {
                    button {
                        text = "📅 Календарь"
                        type = MoneyManagerButtonType.OPEN_CALENDAR
                    }
                }
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_HISTORY
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectEndDateReply() {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_END_DATE

        message {
            val startFormatted = context.historyStartDate?.let {
                dateFormatter.format(it)
            } ?: ""

            text = """
                |📅 Начало: $startFormatted
                |
                |Выберите конец периода:
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "Сегодня"
                        type = MoneyManagerButtonType.QUICK_DATE_TODAY
                    }
                    button {
                        text = "Вчера"
                        type = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
                    }
                }
                buttonRow {
                    button {
                        text = "📅 Календарь"
                        type = MoneyManagerButtonType.OPEN_CALENDAR
                    }
                }
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_HISTORY
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistoryCalendarReply() {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_CALENDAR

        message {
            text = "📅 Выберите дату"
            val year = context.calendarYear
            val month = context.calendarMonth

            keyboard {
                calendar(year, month)
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_HISTORY
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectYearReply() {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_YEAR

        message {
            text = "📅 Выберите год"
            val year = context.calendarYear

            keyboard {
                yearPicker(year)
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_CALENDAR
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectMonthReply() {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_MONTH

        message {
            text = "📅 Выберите месяц"
            val year = context.calendarYear

            keyboard {
                monthPicker(year)
                buttonRow {
                    button {
                        text = "◀\uFE0F Назад"
                        type = MoneyManagerButtonType.BACK_TO_CALENDAR
                    }
                }
            }
        }
    }
}
