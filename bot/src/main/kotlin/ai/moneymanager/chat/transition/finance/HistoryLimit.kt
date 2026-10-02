package ai.moneymanager.chat.transition.finance

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.SubscriptionLimitsService
import java.time.LocalDate

private const val PREMIUM_REQUIRED_KEY = "finance.history.premium_required"

fun SubscriptionLimitsService.premiumRequiredText(
    periodStart: LocalDate,
    userInfo: UserInfo?,
    localizationService: LocalizationService,
): String? {
    val paid = userInfo?.hasActivePaidSubscription() == true
    if (!isBeyondHistoryLimit(periodStart, paid)) return null
    return localizationService.t(PREMIUM_REQUIRED_KEY, userInfo?.language, maxHistoryDaysBack(paid) ?: 0)
}
