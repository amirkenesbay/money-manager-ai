package ai.moneymanager.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

/** Тарифные лимиты (Free/Premium) для функций, отличных от AI-запросов (см. AiRateLimitService для тех). */
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

    /** null означает «без ограничения по глубине истории» (Premium). */
    fun maxHistoryDaysBack(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxHistoryDaysBack

    /** true, если запрошенная дата выходит за пределы разрешённой глубины истории для тира. */
    fun isBeyondHistoryLimit(date: LocalDate, hasPaidSubscription: Boolean): Boolean {
        val maxDaysBack = maxHistoryDaysBack(hasPaidSubscription) ?: return false
        return date.isBefore(LocalDate.now().minusDays(maxDaysBack.toLong()))
    }

    /** null означает «без лимита» (Premium). */
    fun maxOwnedSharedGroups(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxOwnedSharedGroups

    /** null означает «без лимита» (Premium). */
    fun maxActiveNotifications(hasPaidSubscription: Boolean): Int? =
        if (hasPaidSubscription) null else freeMaxActiveNotifications
}
