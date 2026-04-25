package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistoryReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_VIEW

        message {
            val lang = context.userInfo?.language
            text = context.historyReport ?: localizationService.t("finance.history.loading", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.date.button.today", lang)
                        type = MoneyManagerButtonType.HISTORY_DAY_TODAY
                    }
                    button {
                        text = localizationService.t("finance.date.button.yesterday", lang)
                        type = MoneyManagerButtonType.HISTORY_DAY_YESTERDAY
                    }
                    button {
                        text = localizationService.t("finance.date.button.before_yesterday", lang)
                        type = MoneyManagerButtonType.HISTORY_DAY_BEFORE_YESTERDAY
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.history.button.change_period", lang)
                        type = MoneyManagerButtonType.CHANGE_PERIOD
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectStartDateReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE

        message {
            val lang = context.userInfo?.language
            text = localizationService.t("finance.history.start_date.title", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.history.button.this_month", lang)
                        type = MoneyManagerButtonType.QUICK_PERIOD_THIS_MONTH
                    }
                    button {
                        text = localizationService.t("finance.history.button.last_month", lang)
                        type = MoneyManagerButtonType.QUICK_PERIOD_LAST_MONTH
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.history.button.this_year", lang)
                        type = MoneyManagerButtonType.QUICK_PERIOD_THIS_YEAR
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.date.button.calendar", lang)
                        type = MoneyManagerButtonType.OPEN_CALENDAR
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_HISTORY
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectEndDateReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_END_DATE

        message {
            val lang = context.userInfo?.language
            val startFormatted = context.historyStartDate?.let {
                dateFormatter.format(it)
            } ?: ""

            text = localizationService.t("finance.history.end_date.title", lang, startFormatted)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.date.button.today", lang)
                        type = MoneyManagerButtonType.QUICK_DATE_TODAY
                    }
                    button {
                        text = localizationService.t("finance.date.button.yesterday", lang)
                        type = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.date.button.calendar", lang)
                        type = MoneyManagerButtonType.OPEN_CALENDAR
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_HISTORY
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistoryCalendarReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_CALENDAR

        message {
            val lang = context.userInfo?.language
            text = localizationService.t("finance.date.select.title", lang)
            val year = context.calendarYear
            val month = context.calendarMonth

            keyboard {
                calendar(year, month)
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_HISTORY
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectYearReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_YEAR

        message {
            val lang = context.userInfo?.language
            text = localizationService.t("finance.date.year.title", lang)
            val year = context.calendarYear

            keyboard {
                yearPicker(year)
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_CALENDAR
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeHistorySelectMonthReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_HISTORY_SELECT_MONTH

        message {
            val lang = context.userInfo?.language
            text = localizationService.t("finance.date.month.title", lang)
            val year = context.calendarYear

            keyboard {
                monthPicker(year)
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_CALENDAR
                    }
                }
            }
        }
    }
}
