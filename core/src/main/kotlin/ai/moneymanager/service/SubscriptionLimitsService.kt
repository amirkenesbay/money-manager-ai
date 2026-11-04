package ai.moneymanager.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

@Service
class SubscriptionLimitsService(
    @Value("\${ai.rate-limit.daily-requests-per-user:10}")
    private val freeMaxAiRequestsPerDay: Int,
    @Value("\${ai.rate-limit.daily-requests-per-user-paid:100}")
    private val paidMaxAiRequestsPerDay: Int,
    @Value("\${category.max-per-type-free:10}")
    private val freeMaxCategoriesPerType: Int,
    @Value("\${category.max-per-type-paid:1000}")
    private val paidMaxCategoriesPerType: Int,
    @Value("\${history.max-days-back-free:30}")
    private val freeMaxHistoryDaysBack: Int,
    @Value("\${group.max-owned-shared-free:3}")
    private val freeMaxOwnedSharedGroups: Int,
    @Value("\${notification.max-active-free:3}")
    private val freeMaxActiveNotifications: Int,
    @Value("\${budget.max-free:3}")
    private val freeMaxBudgets: Int,
    @Value("\${recurring.max-free:3}")
    private val freeMaxRecurringPayments: Int,
    private val clock: Clock
) {
    fun maxAiRequestsPerDay(hasPaidSubscription: Boolean): Int =
        if (hasPaidSubscription) paidMaxAiRequestsPerDay else freeMaxAiRequestsPerDay

    fun maxCategoriesPerType(hasPaidSubscription: Boolean): Int =
        if (hasPaidSubscription) paidMaxCategoriesPerType else freeMaxCategoriesPerType

    fun maxHistoryDaysBack(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxHistoryDaysBack

    fun earliestHistoryDate(hasPaidSubscription: Boolean): LocalDate? =
        maxHistoryDaysBack(hasPaidSubscription)?.let { LocalDate.now(clock).minusDays(it.toLong()) }

    fun isBeyondHistoryLimit(date: LocalDate, hasPaidSubscription: Boolean): Boolean =
        earliestHistoryDate(hasPaidSubscription)?.let { date.isBefore(it) } ?: false

    fun maxOwnedSharedGroups(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxOwnedSharedGroups

    fun maxActiveNotifications(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxActiveNotifications

    fun maxBudgets(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxBudgets

    fun maxRecurringPayments(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxRecurringPayments
}
