package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.bson.types.ObjectId
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

private const val FIRST_PAGE = 0
private const val DEFAULT_PAGE_SIZE = 20
private const val MAX_PAGE_SIZE = 100
private const val HISTORY_DAYS_BACK_DETAIL = "historyDaysBack"

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}")
class GroupFinanceController(
    private val groupAccessGuard: GroupAccessGuard,
    private val financeOperationService: FinanceOperationService,
    private val subscriptionLimitsService: SubscriptionLimitsService,
    private val financeResponseFactory: FinanceResponseFactory,
) {
    @GetMapping("/balance")
    fun balance(user: UserInfo, @PathVariable groupId: ObjectId): BalanceResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        return financeResponseFactory.balance(group, financeOperationService.calculateBalance(groupId, group.initialBalance))
    }

    @GetMapping("/operations")
    fun operations(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) type: CategoryType?,
        @RequestParam(required = false) categoryId: ObjectId?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): OperationsPageResponse {
        groupAccessGuard.requireMember(user, groupId)
        val pageNumber = page ?: FIRST_PAGE
        val pageSize = size ?: DEFAULT_PAGE_SIZE
        requireValidRequest(from, to, pageNumber, pageSize)
        val filter = OperationFilter(groupId, historyStart(user, from), to, type, categoryId)
        return financeResponseFactory.operationsPage(financeOperationService.findOperations(filter, pageNumber, pageSize))
    }

    private fun historyStart(user: UserInfo, requestedFrom: LocalDate?): LocalDate? {
        val paid = user.hasActivePaidSubscription()
        if (requestedFrom == null) return subscriptionLimitsService.earliestHistoryDate(paid)
        if (!subscriptionLimitsService.isBeyondHistoryLimit(requestedFrom, paid)) return requestedFrom
        val daysBack = checkNotNull(subscriptionLimitsService.maxHistoryDaysBack(paid))
        throw ApiException(
            code = ApiErrorCode.PREMIUM_REQUIRED,
            messageArgs = listOf(daysBack),
            details = mapOf(HISTORY_DAYS_BACK_DETAIL to daysBack),
        )
    }

    private fun requireValidRequest(from: LocalDate?, to: LocalDate?, page: Int, size: Int) {
        val periodValid = from == null || to == null || !from.isAfter(to)
        if (!periodValid || page < FIRST_PAGE || size !in 1..MAX_PAGE_SIZE) {
            throw ApiException(ApiErrorCode.BAD_REQUEST)
        }
    }
}
