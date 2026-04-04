package ai.moneymanager.chat.transition.finance

import ai.moneymanager.chat.reply.common.formatUserDisplayName
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import java.time.LocalDate

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeReportTransitions(
    financeReportService: FinanceReportService,
    userInfoService: UserInfoService,
    categoryService: CategoryService,
    groupService: GroupService
) {
    openReportMenuTransition()
    comparisonTransitions(financeReportService)
    analyticsTransitions(financeReportService)
    membersTransitions(financeReportService, userInfoService, groupService)
    categoryReportTransitions(financeReportService, categoryService)
    reportBackTransitions()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openReportMenuTransition() {
    transition {
        name = "Open report menu"

        condition {
            from = MoneyManagerState.FINANCE_MANAGEMENT
            button = MoneyManagerButtonType.FINANCE_REPORT
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MENU
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.comparisonTransitions(
    financeReportService: FinanceReportService
) {
    transition {
        name = "Open comparison report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.REPORT_COMPARISON
        }

        action {
            context.reportMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1)
            loadComparisonReport(financeReportService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_COMPARISON
        }
    }

    transition {
        name = "Comparison: previous pair"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_COMPARISON
            button = MoneyManagerButtonType.REPORT_PREV
        }

        action {
            context.reportMonth = context.reportMonth?.minusMonths(1)
            loadComparisonReport(financeReportService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_COMPARISON
        }
    }

    transition {
        name = "Comparison: next pair"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_COMPARISON
            button = MoneyManagerButtonType.REPORT_NEXT
        }

        action {
            context.reportMonth = context.reportMonth?.plusMonths(1)
            loadComparisonReport(financeReportService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_COMPARISON
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadComparisonReport(
    financeReportService: FinanceReportService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val month1Start = context.reportMonth ?: return
    context.reportText = financeReportService.generateComparisonReport(groupId, month1Start)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.analyticsTransitions(
    financeReportService: FinanceReportService
) {
    transition {
        name = "Open analytics report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.REPORT_ANALYTICS
        }

        action {
            context.reportMonth = LocalDate.now().withDayOfMonth(1)
            loadAnalyticsReport(financeReportService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_ANALYTICS
        }
    }

    transition {
        name = "Analytics: previous month"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_ANALYTICS
            button = MoneyManagerButtonType.REPORT_PREV
        }

        action {
            context.reportMonth = context.reportMonth?.minusMonths(1)
            loadAnalyticsReport(financeReportService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_ANALYTICS
        }
    }

    transition {
        name = "Analytics: next month"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_ANALYTICS
            button = MoneyManagerButtonType.REPORT_NEXT
        }

        action {
            context.reportMonth = context.reportMonth?.plusMonths(1)
            loadAnalyticsReport(financeReportService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_ANALYTICS
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadAnalyticsReport(
    financeReportService: FinanceReportService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val monthStart = context.reportMonth ?: return
    context.reportText = financeReportService.generateAnalyticsReport(groupId, monthStart)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.membersTransitions(
    financeReportService: FinanceReportService,
    userInfoService: UserInfoService,
    groupService: GroupService
) {
    transition {
        name = "Open members report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.REPORT_BY_MEMBERS
        }

        action {
            context.reportMonth = LocalDate.now().withDayOfMonth(1)
            loadMembersReport(financeReportService, userInfoService, groupService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MEMBERS
        }
    }

    transition {
        name = "Members: previous month"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MEMBERS
            button = MoneyManagerButtonType.REPORT_PREV
        }

        action {
            context.reportMonth = context.reportMonth?.minusMonths(1)
            loadMembersReport(financeReportService, userInfoService, groupService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MEMBERS
        }
    }

    transition {
        name = "Members: next month"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MEMBERS
            button = MoneyManagerButtonType.REPORT_NEXT
        }

        action {
            context.reportMonth = context.reportMonth?.plusMonths(1)
            loadMembersReport(financeReportService, userInfoService, groupService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MEMBERS
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadMembersReport(
    financeReportService: FinanceReportService,
    userInfoService: UserInfoService,
    groupService: GroupService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val monthStart = context.reportMonth ?: return

    val group = groupService.getGroup(groupId)
    val memberIds = group?.memberIds ?: setOf(user.id)

    val memberNames = memberIds.associateWith { memberId ->
        val info = userInfoService.getUserInfoByTelegramId(memberId)
        formatUserDisplayName(info, memberId)
    }

    context.reportText = financeReportService.generateMembersReport(groupId, monthStart, memberNames)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryReportTransitions(
    financeReportService: FinanceReportService,
    categoryService: CategoryService
) {
    transition {
        name = "Open category select for report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.REPORT_BY_CATEGORY
        }

        action {
            val groupId = context.userInfo?.activeGroupId ?: return@action
            context.categories = categoryService.getCategoriesByGroup(groupId)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_CATEGORY_SELECT
        }
    }

    transition {
        name = "Select category for report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_CATEGORY_SELECT
            button = MoneyManagerButtonType.REPORT_CATEGORY_ITEM
        }

        action {
            val selectedName = buttonText?.replaceFirst(Regex("^\\S+\\s"), "") ?: return@action
            val category = context.categories.find { it.name == selectedName } ?: return@action
            context.reportCategory = category

            val groupId = context.userInfo?.activeGroupId ?: return@action
            val categoryId = category.id ?: return@action
            context.reportText = financeReportService.generateCategoryReport(
                groupId, categoryId, category.icon, category.name
            )
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_CATEGORY_VIEW
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.reportBackTransitions() {
    transition {
        name = "Back to finance from report menu"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.BACK_TO_FINANCE
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }

    transition {
        name = "Back to report menu from comparison"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_COMPARISON
            button = MoneyManagerButtonType.BACK_TO_REPORT_MENU
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MENU
        }
    }

    transition {
        name = "Back to report menu from analytics"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_ANALYTICS
            button = MoneyManagerButtonType.BACK_TO_REPORT_MENU
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MENU
        }
    }

    transition {
        name = "Back to report menu from members"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MEMBERS
            button = MoneyManagerButtonType.BACK_TO_REPORT_MENU
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MENU
        }
    }

    transition {
        name = "Back to report menu from category select"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_CATEGORY_SELECT
            button = MoneyManagerButtonType.BACK_TO_REPORT_MENU
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MENU
        }
    }

    transition {
        name = "Back to report menu from category view"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_CATEGORY_VIEW
            button = MoneyManagerButtonType.BACK_TO_REPORT_MENU
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MENU
        }
    }
}