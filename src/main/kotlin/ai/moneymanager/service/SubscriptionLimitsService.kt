package ai.moneymanager.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SubscriptionLimitsService(
    @Value("\${category.max-per-type-free:10}")
    private val freeMaxCategoriesPerType: Int,
    @Value("\${category.max-per-type-paid:1000}")
    private val paidMaxCategoriesPerType: Int,
    @Value("\${history.max-days-back-free:30}")
    private val freeMaxHistoryDaysBack: Int,
    @Value("\${group.max-owned-shared-free:3}")
    private val freeMaxOwnedSharedGroups: Int,
    @Value("\${notification.max-active-free:3}")
    private val freeMaxActiveNotifications: Int
) {
    fun maxCategoriesPerType(hasPaidSubscription: Boolean): Int =
        if (hasPaidSubscription) paidMaxCategoriesPerType else freeMaxCategoriesPerType

    fun maxHistoryDaysBack(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxHistoryDaysBack

    fun isBeyondHistoryLimit(date: LocalDate, hasPaidSubscription: Boolean): Boolean {
        val maxDaysBack = maxHistoryDaysBack(hasPaidSubscription) ?: return false
        return date.isBefore(LocalDate.now().minusDays(maxDaysBack.toLong()))
    }

    fun maxOwnedSharedGroups(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxOwnedSharedGroups

    fun maxActiveNotifications(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxActiveNotifications
}
