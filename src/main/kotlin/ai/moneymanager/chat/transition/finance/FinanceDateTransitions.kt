package ai.moneymanager.chat.transition.finance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.FinanceOperationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.widget.CalendarButtonType
import java.time.LocalDate

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeDateTransitions(
    financeOperationService: FinanceOperationService
) {
    openCalendarTransition()
    calendarMonthNavigationTransitions()
    calendarYearNavigationTransitions()
    calendarYearPickerTransitions()
    calendarMonthPickerTransitions()
    selectDayTransition(financeOperationService)
    quickDateTransitions(financeOperationService)
    calendarBackTransitions()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openCalendarTransition() {
    transition {
        name = "Open calendar from date selection"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_DATE
            button = MoneyManagerButtonType.OPEN_CALENDAR
        }

        action {
            val now = LocalDate.now()
            context.calendarYear = now.year
            context.calendarMonth = now.monthValue
        }

        then {
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.calendarMonthNavigationTransitions() {
    transition {
        name = "Calendar: previous month"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
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
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }

    transition {
        name = "Calendar: next month"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
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
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.calendarYearNavigationTransitions() {
    transition {
        name = "Calendar: previous year"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
            button = CalendarButtonType.CALENDAR_PREV_YEAR
        }

        action {
            val data = buttonText
            if (data != null) {
                context.calendarYear = data.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }

    transition {
        name = "Calendar: next year"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
            button = CalendarButtonType.CALENDAR_NEXT_YEAR
        }

        action {
            val data = buttonText
            if (data != null) {
                context.calendarYear = data.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.calendarYearPickerTransitions() {
    transition {
        name = "Calendar: open year picker"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
            button = CalendarButtonType.CALENDAR_SELECT_YEAR
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_YEAR
        }
    }

    transition {
        name = "Calendar: select year"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_YEAR
            button = CalendarButtonType.CALENDAR_YEAR
        }

        action {
            val yearText = buttonText
            if (yearText != null) {
                context.calendarYear = yearText.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.calendarMonthPickerTransitions() {
    transition {
        name = "Calendar: open month picker"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
            button = CalendarButtonType.CALENDAR_SELECT_MONTH
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_MONTH
        }
    }

    transition {
        name = "Calendar: select month"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_MONTH
            button = CalendarButtonType.CALENDAR_MONTH
        }

        action {
            val data = buttonText
            if (data != null) {
                context.calendarMonth = data.toInt()
            }
        }

        then {
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.selectDayTransition(
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Calendar: select day and save operation"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
            button = CalendarButtonType.CALENDAR_DAY
        }

        action {
            val dayText = buttonText
            if (dayText != null) {
                context.selectedDate = LocalDate.of(
                    context.calendarYear,
                    context.calendarMonth,
                    dayText.toInt()
                )
            }
            saveFinanceOperation(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_OPERATION_SAVED
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.quickDateTransitions(
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Quick date: today"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_DATE
            button = MoneyManagerButtonType.QUICK_DATE_TODAY
        }

        action {
            context.selectedDate = LocalDate.now()
            saveFinanceOperation(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_OPERATION_SAVED
        }
    }

    transition {
        name = "Quick date: yesterday"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_DATE
            button = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
        }

        action {
            context.selectedDate = LocalDate.now().minusDays(1)
            saveFinanceOperation(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_OPERATION_SAVED
        }
    }

    transition {
        name = "Quick date: before yesterday"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_DATE
            button = MoneyManagerButtonType.QUICK_DATE_BEFORE_YESTERDAY
        }

        action {
            context.selectedDate = LocalDate.now().minusDays(2)
            saveFinanceOperation(financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_OPERATION_SAVED
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.saveFinanceOperation(
    financeOperationService: FinanceOperationService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val type = context.financeOperationType ?: return
    val amount = context.financeAmount ?: return
    val category = context.selectedCategory ?: return
    val date = context.selectedDate ?: return

    financeOperationService.save(
        groupId = groupId,
        creatorId = user.id,
        type = type,
        amount = amount,
        categoryId = category.id!!,
        categoryName = category.name,
        categoryIcon = category.icon,
        operationDate = date,
        description = context.financeComment
    )
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.calendarBackTransitions() {
    transition {
        name = "Back to finance from date selection"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_DATE
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }

    transition {
        name = "Back to date selection from calendar"

        condition {
            from = MoneyManagerState.FINANCE_CALENDAR
            button = MoneyManagerButtonType.BACK_TO_FINANCE_DATE
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_DATE
        }
    }

    transition {
        name = "Back to calendar from year picker"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_YEAR
            button = MoneyManagerButtonType.BACK_TO_CALENDAR
        }

        then {
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }

    transition {
        name = "Back to calendar from month picker"

        condition {
            from = MoneyManagerState.FINANCE_SELECT_MONTH
            button = MoneyManagerButtonType.BACK_TO_CALENDAR
        }

        then {
            to = MoneyManagerState.FINANCE_CALENDAR
        }
    }
}
