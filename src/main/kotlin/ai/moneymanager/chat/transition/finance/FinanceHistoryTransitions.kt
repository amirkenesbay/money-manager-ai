package ai.moneymanager.chat.transition.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.FinanceOperationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.widget.CalendarButtonType
import java.time.LocalDate

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeHistoryTransitions(
    financeOperationService: FinanceOperationService
) {
    openHistoryTransition(financeOperationService)
    changePeriodTransitions(financeOperationService)
    historyCalendarTransitions()
    historyBackTransitions()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openHistoryTransition(
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Open finance history"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            button = MoneyManagerButtonType.FINANCE_HISTORY
        }

        action {
            val now = LocalDate.now()
            context.historyStartDate = now.withDayOfMonth(1)
            context.historyEndDate = now.withDayOfMonth(now.lengthOfMonth())
            loadReport(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.changePeriodTransitions(
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Open period selection"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_VIEW
            button = MoneyManagerButtonType.CHANGE_PERIOD
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE
        }
    }

    // Quick periods
    transition {
        name = "Quick period: this month"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE
            button = MoneyManagerButtonType.QUICK_PERIOD_THIS_MONTH
        }

        action {
            val now = LocalDate.now()
            context.historyStartDate = now.withDayOfMonth(1)
            context.historyEndDate = now.withDayOfMonth(now.lengthOfMonth())
            loadReport(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    transition {
        name = "Quick period: last month"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE
            button = MoneyManagerButtonType.QUICK_PERIOD_LAST_MONTH
        }

        action {
            val lastMonth = LocalDate.now().minusMonths(1)
            context.historyStartDate = lastMonth.withDayOfMonth(1)
            context.historyEndDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
            loadReport(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    transition {
        name = "Quick period: this year"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE
            button = MoneyManagerButtonType.QUICK_PERIOD_THIS_YEAR
        }

        action {
            val now = LocalDate.now()
            context.historyStartDate = now.withDayOfYear(1)
            context.historyEndDate = now.withMonth(12).withDayOfMonth(31)
            loadReport(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    // Calendar flow: start date
    transition {
        name = "Open calendar for start date"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE
            button = MoneyManagerButtonType.OPEN_CALENDAR
        }

        action {
            context.historySelectingStart = true
            val now = LocalDate.now()
            context.calendarYear = now.year
            context.calendarMonth = now.monthValue
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "Select start date from calendar"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_DAY
            guard { context.historySelectingStart }
        }

        action {
            val dayText = buttonText
            if (dayText != null) {
                context.historyStartDate = LocalDate.of(
                    context.calendarYear,
                    context.calendarMonth,
                    dayText.toInt()
                )
            }
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_SELECT_END_DATE
        }
    }

    // End date selection
    transition {
        name = "Quick end date: today"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_END_DATE
            button = MoneyManagerButtonType.QUICK_DATE_TODAY
        }

        action {
            context.historyEndDate = LocalDate.now()
            loadReport(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    transition {
        name = "Quick end date: yesterday"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_END_DATE
            button = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
        }

        action {
            context.historyEndDate = LocalDate.now().minusDays(1)
            loadReport(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    transition {
        name = "Open calendar for end date"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_END_DATE
            button = MoneyManagerButtonType.OPEN_CALENDAR
        }

        action {
            context.historySelectingStart = false
            val now = LocalDate.now()
            context.calendarYear = now.year
            context.calendarMonth = now.monthValue
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "Select end date from calendar"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_DAY
            guard { !context.historySelectingStart }
        }

        action {
            val dayText = buttonText
            if (dayText != null) {
                context.historyEndDate = LocalDate.of(
                    context.calendarYear,
                    context.calendarMonth,
                    dayText.toInt()
                )
            }
            loadReport(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.historyCalendarTransitions() {
    transition {
        name = "History calendar: previous month"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_PREV_MONTH
        }

        action {
            val data = buttonText
            if (data != null) {
                val parts = data.split(":")
                context.calendarYear = parts[0].toInt()
                context.calendarMonth = parts[1].toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "History calendar: next month"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_NEXT_MONTH
        }

        action {
            val data = buttonText
            if (data != null) {
                val parts = data.split(":")
                context.calendarYear = parts[0].toInt()
                context.calendarMonth = parts[1].toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "History calendar: previous year"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_PREV_YEAR
        }

        action {
            val data = buttonText
            if (data != null) {
                context.calendarYear = data.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "History calendar: next year"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_NEXT_YEAR
        }

        action {
            val data = buttonText
            if (data != null) {
                context.calendarYear = data.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "History calendar: open year picker"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_SELECT_YEAR
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_SELECT_YEAR
        }
    }

    transition {
        name = "History calendar: select year"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_YEAR
            button = CalendarButtonType.CALENDAR_YEAR
        }

        action {
            val yearText = buttonText
            if (yearText != null) {
                context.calendarYear = yearText.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "History calendar: open month picker"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = CalendarButtonType.CALENDAR_SELECT_MONTH
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_SELECT_MONTH
        }
    }

    transition {
        name = "History calendar: select month"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_MONTH
            button = CalendarButtonType.CALENDAR_MONTH
        }

        action {
            val data = buttonText
            if (data != null) {
                context.calendarMonth = data.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.historyBackTransitions() {
    transition {
        name = "Back to finance from history"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_VIEW
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }

    transition {
        name = "Back to history from start date selection"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_START_DATE
            button = MoneyManagerButtonType.BACK_TO_HISTORY
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    transition {
        name = "Back to history from end date selection"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_END_DATE
            button = MoneyManagerButtonType.BACK_TO_HISTORY
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    transition {
        name = "Back to history from calendar"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_CALENDAR
            button = MoneyManagerButtonType.BACK_TO_HISTORY
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_VIEW
        }
    }

    transition {
        name = "Back to calendar from history year picker"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_YEAR
            button = MoneyManagerButtonType.BACK_TO_CALENDAR
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }

    transition {
        name = "Back to calendar from history month picker"

        condition {
            from = MoneyManagerState.FINANCE_HISTORY_SELECT_MONTH
            button = MoneyManagerButtonType.BACK_TO_CALENDAR
        }

        then {
            to = MoneyManagerState.FINANCE_HISTORY_CALENDAR
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadReport(
    financeOperationService: FinanceOperationService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val startDate = context.historyStartDate ?: return
    val endDate = context.historyEndDate ?: return
    context.historyReport = financeOperationService.generateReport(groupId, startDate, endDate)
}
