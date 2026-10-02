package ai.moneymanager.web.report

import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.HistoryAccessPolicy
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
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
private const val DEFAULT_CATEGORY_MONTHS = 6
private const val MAX_CATEGORY_MONTHS = 12
private const val CATEGORY_NOT_FOUND_KEY = "${API_ERROR_KEY_PREFIX}category_not_found"

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/reports")
class ReportController(
    private val groupAccessGuard: GroupAccessGuard,
    private val historyAccessPolicy: HistoryAccessPolicy,
    private val financeReportService: FinanceReportService,
    private val categoryService: CategoryService,
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
        val report = financeReportService.buildAnalyticsReport(groupId, scope.month.atDay(1), user.language)
        return reportResponseFactory.analytics(report, scope.month, scope.group.currency)
    }

    @GetMapping("/comparison")
    fun comparison(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam(required = false) @DateTimeFormat(pattern = MONTH_PATTERN) month: YearMonth?,
    ): ComparisonReportResponse {
        val scope = monthScope(user, groupId, month, ::previousMonthStart)
        val report = financeReportService.buildComparisonReport(groupId, previousMonthStart(scope.month), user.language)
        return reportResponseFactory.comparison(report, scope.month, scope.group.currency)
    }

    @GetMapping("/members")
    fun members(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam(required = false) @DateTimeFormat(pattern = MONTH_PATTERN) month: YearMonth?,
    ): MembersReportResponse {
        val scope = monthScope(user, groupId, month)
        val memberNames = userInfoService.displayNames(scope.group.memberIds)
        val report = financeReportService.buildMembersReport(groupId, scope.month.atDay(1), memberNames, user.language)
        return reportResponseFactory.members(report, scope.month, scope.group.currency)
    }

    @GetMapping("/category")
    fun category(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam categoryId: ObjectId,
        @RequestParam(required = false) months: Int?,
    ): CategoryReportResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val monthCount = months ?: DEFAULT_CATEGORY_MONTHS
        if (monthCount !in 1..MAX_CATEGORY_MONTHS) throw ApiException(ApiErrorCode.BAD_REQUEST)
        val category = categoryService.getCategory(categoryId)?.takeIf { it.groupId == groupId }
            ?: throw ApiException(ApiErrorCode.NOT_FOUND, CATEGORY_NOT_FOUND_KEY)
        historyAccessPolicy.requireWithinHistory(user, YearMonth.now(clock).minusMonths(monthCount - 1L).atDay(1))
        val report = financeReportService.buildCategoryReport(groupId, categoryId, category.icon, category.name, monthCount, user.language)
        return reportResponseFactory.category(report, categoryId, group.currency)
    }

    private fun monthScope(
        user: UserInfo,
        groupId: ObjectId,
        month: YearMonth?,
        periodStartOf: (YearMonth) -> LocalDate = { it.atDay(1) },
    ): MonthScope {
        val group = groupAccessGuard.requireMember(user, groupId)
        val reportMonth = month ?: YearMonth.now(clock)
        historyAccessPolicy.requireWithinHistory(user, periodStartOf(reportMonth))
        return MonthScope(group, reportMonth)
    }
}

private data class MonthScope(val group: MoneyGroup, val month: YearMonth)

private fun previousMonthStart(month: YearMonth): LocalDate = month.minusMonths(1).atDay(1)
