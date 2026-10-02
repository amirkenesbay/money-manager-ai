package ai.moneymanager.web.access

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.springframework.stereotype.Component
import java.time.LocalDate

private const val HISTORY_DAYS_BACK_DETAIL = "historyDaysBack"

@Component
class HistoryAccessPolicy(
    private val subscriptionLimitsService: SubscriptionLimitsService,
) {
    fun earliestAllowedDate(user: UserInfo): LocalDate? =
        subscriptionLimitsService.earliestHistoryDate(user.hasActivePaidSubscription())

    fun requireWithinHistory(user: UserInfo, periodStart: LocalDate) {
        val paid = user.hasActivePaidSubscription()
        if (!subscriptionLimitsService.isBeyondHistoryLimit(periodStart, paid)) return
        val daysBack = checkNotNull(subscriptionLimitsService.maxHistoryDaysBack(paid))
        throw ApiException(
            code = ApiErrorCode.PREMIUM_REQUIRED,
            messageArgs = listOf(daysBack),
            details = mapOf(HISTORY_DAYS_BACK_DETAIL to daysBack),
        )
    }
}
