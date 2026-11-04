package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.RecurringMode
import ai.moneymanager.domain.model.RecurringSaveResult
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.entity.RecurringPaymentEntity
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.limitReached
import ai.moneymanager.web.time.UserCalendar
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

private const val RECURRING_NOT_FOUND_KEY = "${API_ERROR_KEY_PREFIX}recurring_not_found"

data class RecurringRequest(
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryId: ObjectId,
    val description: String?,
    val unit: RecurrenceUnit,
    val interval: Int = 1,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val mode: RecurringMode,
    val firstRecorded: Boolean = false,
)

data class RecurringActiveRequest(
    val active: Boolean,
)

data class RecurringResponse(
    val id: ObjectId,
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryId: ObjectId,
    val categoryName: String,
    val categoryIcon: String,
    val description: String?,
    val unit: RecurrenceUnit,
    val interval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val nextDate: LocalDate,
    val mode: RecurringMode,
    val active: Boolean,
    val monthlyAmount: BigDecimal,
)

data class RecurringListResponse(
    val items: List<RecurringResponse>,
    val monthlyExpense: BigDecimal,
    val monthlyIncome: BigDecimal,
)

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/recurring")
class RecurringController(
    private val groupAccessGuard: GroupAccessGuard,
    private val recurringPaymentService: RecurringPaymentService,
    private val operationDraftFactory: OperationDraftFactory,
    private val subscriptionLimitsService: SubscriptionLimitsService,
    private val userCalendar: UserCalendar,
) {
    @GetMapping
    fun payments(user: UserInfo, @PathVariable groupId: ObjectId): RecurringListResponse {
        groupAccessGuard.requireMember(user, groupId)
        val items = recurringPaymentService.payments(groupId).map { it.toResponse() }
        return RecurringListResponse(items, monthlyTotal(items, CategoryType.EXPENSE), monthlyTotal(items, CategoryType.INCOME))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: RecurringRequest): RecurringResponse {
        groupAccessGuard.requireMember(user, groupId)
        val draft = operationDraftFactory.recurring(groupId, request)
        return saved(recurringPaymentService.create(actorOf(user), groupId, draft, request.firstRecorded, maxActive(user), userCalendar.today(user)))
    }

    @PutMapping("/{paymentId}")
    fun update(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable paymentId: ObjectId, @RequestBody request: RecurringRequest): RecurringResponse {
        groupAccessGuard.requireMember(user, groupId)
        val draft = operationDraftFactory.recurring(groupId, request)
        return saved(recurringPaymentService.update(groupId, paymentId, draft, userCalendar.today(user)))
    }

    @PatchMapping("/{paymentId}")
    fun setActive(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable paymentId: ObjectId, @RequestBody request: RecurringActiveRequest): RecurringResponse {
        groupAccessGuard.requireMember(user, groupId)
        return saved(recurringPaymentService.setActive(actorOf(user), groupId, paymentId, request.active, maxActive(user), userCalendar.today(user)))
    }

    @DeleteMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable paymentId: ObjectId) {
        groupAccessGuard.requireMember(user, groupId)
        if (!recurringPaymentService.delete(groupId, paymentId)) throw notFound()
    }

    private fun saved(result: RecurringSaveResult): RecurringResponse = when (result) {
        is RecurringSaveResult.Saved -> result.payment.toResponse()
        is RecurringSaveResult.LimitReached -> throw limitReached(ApiErrorCode.RECURRING_LIMIT_REACHED, result.limit)
        RecurringSaveResult.NotFound -> throw notFound()
    }

    private fun actorOf(user: UserInfo): Long = checkNotNull(user.telegramUserId)

    private fun maxActive(user: UserInfo): Int? = subscriptionLimitsService.maxRecurringPayments(user.hasActivePaidSubscription())

    private fun notFound() = ApiException(ApiErrorCode.NOT_FOUND, RECURRING_NOT_FOUND_KEY)

    private fun monthlyTotal(items: List<RecurringResponse>, type: CategoryType): BigDecimal =
        items.filter { it.active && it.type == type }.fold(BigDecimal.ZERO) { total, item -> total + item.monthlyAmount }

    private fun RecurringPaymentEntity.toResponse() = RecurringResponse(
        id = checkNotNull(id),
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon ?: DEFAULT_CATEGORY_ICON,
        description = description,
        unit = unit,
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        nextDate = nextDate,
        mode = mode,
        active = active,
        monthlyAmount = recurrence.monthly(amount),
    )
}
