package ai.moneymanager.chat.transition.finance

import ai.moneymanager.service.CATEGORY_REPORT_DEFAULT_MONTHS
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.chat.reply.common.resolveCurrency
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.chat.state.MoneyManagerButtonType
import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.chat.reply.finance.FinanceReportFormatter
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import java.time.LocalDate

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.financeReportTransitions(
    financeReportFormatter: FinanceReportFormatter,
    financeReportService: FinanceReportService,
    userInfoService: UserInfoService,
    categoryService: CategoryService,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
) {
    openReportMenuTransition()
    comparisonTransitions(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
    analyticsTransitions(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
    membersTransitions(financeReportFormatter, userInfoService, groupService, subscriptionLimitsService, localizationService)
    categoryReportTransitions(financeReportFormatter, financeReportService, categoryService, groupService, subscriptionLimitsService, localizationService)
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
    financeReportFormatter: FinanceReportFormatter,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
) {
    transition {
        name = "Open comparison report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.REPORT_COMPARISON
        }

        action {
            context.reportMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1)
            loadComparisonReport(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
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
            loadComparisonReport(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
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
            loadComparisonReport(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_COMPARISON
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadComparisonReport(
    financeReportFormatter: FinanceReportFormatter,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val month1Start = context.reportMonth ?: return
    val language = context.userInfo?.language

    subscriptionLimitsService.premiumRequiredText(month1Start, context.userInfo, localizationService)?.let {
        context.reportText = it
        return
    }

    context.reportText = financeReportFormatter.generateComparisonReport(groupId, month1Start, resolveCurrency(groupService, groupId), language)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.analyticsTransitions(
    financeReportFormatter: FinanceReportFormatter,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
) {
    transition {
        name = "Open analytics report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.REPORT_ANALYTICS
        }

        action {
            context.reportMonth = LocalDate.now().withDayOfMonth(1)
            loadAnalyticsReport(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
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
            loadAnalyticsReport(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
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
            loadAnalyticsReport(financeReportFormatter, groupService, subscriptionLimitsService, localizationService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_ANALYTICS
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadAnalyticsReport(
    financeReportFormatter: FinanceReportFormatter,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val monthStart = context.reportMonth ?: return
    val language = context.userInfo?.language

    subscriptionLimitsService.premiumRequiredText(monthStart, context.userInfo, localizationService)?.let {
        context.reportText = it
        return
    }

    context.reportText = financeReportFormatter.generateAnalyticsReport(groupId, monthStart, resolveCurrency(groupService, groupId), language)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.membersTransitions(
    financeReportFormatter: FinanceReportFormatter,
    userInfoService: UserInfoService,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
) {
    transition {
        name = "Open members report"

        condition {
            from = MoneyManagerState.FINANCE_REPORT_MENU
            button = MoneyManagerButtonType.REPORT_BY_MEMBERS
        }

        action {
            context.reportMonth = LocalDate.now().withDayOfMonth(1)
            loadMembersReport(financeReportFormatter, userInfoService, groupService, subscriptionLimitsService, localizationService)
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
            loadMembersReport(financeReportFormatter, userInfoService, groupService, subscriptionLimitsService, localizationService)
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
            loadMembersReport(financeReportFormatter, userInfoService, groupService, subscriptionLimitsService, localizationService)
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MEMBERS
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadMembersReport(
    financeReportFormatter: FinanceReportFormatter,
    userInfoService: UserInfoService,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
) {
    val groupId = context.userInfo?.activeGroupId ?: return
    val monthStart = context.reportMonth ?: return
    val language = context.userInfo?.language

    subscriptionLimitsService.premiumRequiredText(monthStart, context.userInfo, localizationService)?.let {
        context.reportText = it
        return
    }

    val group = groupService.getGroup(groupId)
    val memberIds = group?.memberIds ?: setOf(user.id)

    val memberNames = userInfoService.displayNames(memberIds)

    context.reportText = financeReportFormatter.generateMembersReport(groupId, monthStart, memberNames, group?.currency ?: Currency.DEFAULT, language)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryReportTransitions(
    financeReportFormatter: FinanceReportFormatter,
    financeReportService: FinanceReportService,
    categoryService: CategoryService,
    groupService: GroupService,
    subscriptionLimitsService: SubscriptionLimitsService,
    localizationService: LocalizationService
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
            val language = context.userInfo?.language
            val periodStart = financeReportService.categoryReportStart(CATEGORY_REPORT_DEFAULT_MONTHS)
            subscriptionLimitsService.premiumRequiredText(periodStart, context.userInfo, localizationService)?.let {
                context.reportText = it
                return@action
            }
            context.reportText = financeReportFormatter.generateCategoryReport(
                groupId, categoryId, category.icon, category.name, resolveCurrency(groupService, groupId), language
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
