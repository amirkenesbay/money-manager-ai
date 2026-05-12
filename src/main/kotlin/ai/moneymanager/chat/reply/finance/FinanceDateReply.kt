package ai.moneymanager.chat.reply.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectDateReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_DATE

        message {
            val lang = context.userInfo?.language
            text = localizationService.t("finance.date.select.title", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.date.button.before_yesterday", lang)
                        type = MoneyManagerButtonType.QUICK_DATE_BEFORE_YESTERDAY
                    }
                    button {
                        text = localizationService.t("finance.date.button.yesterday", lang)
                        type = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
                    }
                    button {
                        text = localizationService.t("finance.date.button.today", lang)
                        type = MoneyManagerButtonType.QUICK_DATE_TODAY
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
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeCalendarReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_CALENDAR

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
                        type = MoneyManagerButtonType.BACK_TO_FINANCE_DATE
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectYearReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_YEAR

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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeSelectMonthReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_SELECT_MONTH

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
