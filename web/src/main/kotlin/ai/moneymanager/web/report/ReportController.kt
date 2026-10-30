package ai.moneymanager.web.report

import ai.moneymanager.service.CATEGORY_REPORT_DEFAULT_MONTHS
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.domain.model.currentMonthPeriod
import ai.moneymanager.domain.model.monthPeriod
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.access.HistoryAccessPolicy
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.bson.types.ObjectId
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

private const val MONTH_PATTERN = "yyyy-MM"
private const val MAX_CATEGORY_MONTHS = 12
private const val PREVIOUS_MONTH = 1L

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/reports")
class ReportController(
    private val groupAccessGuard: GroupAccessGuard,
    private val historyAccessPolicy: HistoryAccessPolicy,
    private val financeReportService: FinanceReportService,
    private val groupCategoryLookup: GroupCategoryLookup,
    private val userInfoService: UserInfoService,
    private val reportResponseFactory: ReportResponseFactory,
    private val clock: Clock,
) {
    @GetMapping("/analytics")
    fun analytics(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam(required = false) @DateTimeFormat(pattern = MONTH_PATTERN) month: YearMonth?,
    ): AnalyticsReportResponse {
        val scope = monthScope(user, groupId, month)
        val report = financeReportService.buildAnalyticsReport(groupId, scope.period.start, user.language)
        return reportResponseFactory.analytics(report, scope.period, scope.group.currency)
    }

    @GetMapping("/comparison")
    fun comparison(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam(required = false) @DateTimeFormat(pattern = MONTH_PATTERN) month: YearMonth?,
    ): ComparisonReportResponse {
        val scope = monthScope(user, groupId, month, PREVIOUS_MONTH)
        val report = financeReportService.buildComparisonReport(groupId, scope.historyStart, user.language)
        return reportResponseFactory.comparison(report, scope.period, scope.group.currency)
    }

    @GetMapping("/members")
    fun members(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam(required = false) @DateTimeFormat(pattern = MONTH_PATTERN) month: YearMonth?,
    ): MembersReportResponse {
        val scope = monthScope(user, groupId, month)
        val memberNames = userInfoService.displayNames(scope.group.memberIds)
        val report = financeReportService.buildMembersReport(groupId, scope.period.start, memberNames, user.language)
        return reportResponseFactory.members(report, scope.period, scope.group.currency)
    }

    @GetMapping("/category")
    fun category(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam categoryId: ObjectId,
        @RequestParam(required = false) months: Int?,
    ): CategoryReportResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val monthCount = months ?: CATEGORY_REPORT_DEFAULT_MONTHS
        if (monthCount !in 1..MAX_CATEGORY_MONTHS) throw ApiException(ApiErrorCode.BAD_REQUEST)
        val category = groupCategoryLookup.requireCategory(groupId, categoryId)
        historyAccessPolicy.requireWithinHistory(user, financeReportService.categoryReportStart(monthCount))
        val report = financeReportService.buildCategoryReport(groupId, categoryId, category.icon, category.name, monthCount, user.language)
        return reportResponseFactory.category(report, categoryId, group.currency)
    }

    private fun monthScope(user: UserInfo, groupId: ObjectId, month: YearMonth?, monthsBack: Long = 0): MonthScope {
        val group = groupAccessGuard.requireMember(user, groupId)
        val period = month?.let { monthPeriod(it, group.monthStartDay) } ?: currentMonthPeriod(LocalDate.now(clock), group.monthStartDay)
        val historyStart = monthPeriod(period.month.minusMonths(monthsBack), group.monthStartDay).start
        historyAccessPolicy.requireWithinHistory(user, historyStart)
        return MonthScope(group, period, historyStart)
    }
}

private data class MonthScope(val group: MoneyGroup, val period: MonthPeriod, val historyStart: LocalDate)
