package ai.moneymanager.web.me

import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.SubscriptionLimitsService
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class MeResponseFactory(
    private val subscriptionLimitsService: SubscriptionLimitsService,
    private val localizationService: LocalizationService,
) {
    fun create(user: UserInfo): MeResponse = MeResponse(
        userId = checkNotNull(user.telegramUserId),
        firstName = user.firstName,
        username = user.username,
        language = localizationService.resolveLanguage(user.language, user.languageCode),
        timezone = user.timezone,
        activeGroupId = user.activeGroupId,
        subscription = subscriptionOf(user),
    )

    private fun subscriptionOf(user: UserInfo): SubscriptionResponse {
        val paid = user.hasActivePaidSubscription()
        return SubscriptionResponse(
            tier = if (paid) SubscriptionTier.PAID else SubscriptionTier.FREE,
            expiresAt = user.subscriptionExpiresAt?.takeIf { paid }?.atZone(ZoneId.systemDefault())?.toInstant(),
            limits = limitsOf(paid),
        )
    }

    private fun limitsOf(paid: Boolean) = LimitsResponse(
        aiRequestsPerDay = subscriptionLimitsService.maxAiRequestsPerDay(paid),
        categoriesPerType = subscriptionLimitsService.maxCategoriesPerType(paid),
        ownedSharedGroups = subscriptionLimitsService.maxOwnedSharedGroups(paid),
        activeNotifications = subscriptionLimitsService.maxActiveNotifications(paid),
        historyDaysBack = subscriptionLimitsService.maxHistoryDaysBack(paid),
    )
}
