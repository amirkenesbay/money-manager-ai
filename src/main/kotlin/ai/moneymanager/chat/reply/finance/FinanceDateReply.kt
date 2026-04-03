package ai.moneymanager.chat.reply.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectDateReply() {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_DATE

        message {
            text = "📅 Выберите дату"

            keyboard {
                buttonRow {
                    button {
                        text = "Позавчера"
                        type = MoneyManagerButtonType.QUICK_DATE_BEFORE_YESTERDAY
                    }
                    button {
                        text = "Вчера"
                        type = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
                    }
                    button {
                        text = "Сегодня"
                        type = MoneyManagerButtonType.QUICK_DATE_TODAY
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
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeCalendarReply() {
    reply {
        state = MoneyManagerState.FINANCE_CALENDAR

        message {
            text = "📅 Выберите дату"
            val year = context.calendarYear
            val month = context.calendarMonth

            keyboard {
                calendar(year, month)
                buttonRow {
                    button {
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_FINANCE_DATE
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectYearReply() {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_YEAR

        message {
            text = "📅 Выберите год"
            val year = context.calendarYear

            keyboard {
                yearPicker(year)
                buttonRow {
                    button {
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_CALENDAR
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectMonthReply() {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_MONTH

        message {
            text = "📅 Выберите месяц"
            val year = context.calendarYear

            keyboard {
                monthPicker(year)
                buttonRow {
                    button {
                        text = "◀️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_CALENDAR
                    }
                }
            }
        }
    }
}