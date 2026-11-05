package ai.moneymanager.chat.transition.finance

import ai.moneymanager.testing.TEST_FREE_HISTORY_DAYS
import ai.moneymanager.testing.testSubscriptionLimits
import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.testing.testLocalizationService
import ai.moneymanager.testing.testUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val FREE_HISTORY_DAYS = TEST_FREE_HISTORY_DAYS
private val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
private val SIX_MONTHS_AGO: LocalDate = LocalDate.of(2026, 5, 1)

class HistoryLimitTest {
    private val localization = testLocalizationService()
    private val limits = testSubscriptionLimits(TODAY)

    @Test
    fun `asks free users for premium beyond their history`() {
        val text = limits.premiumRequiredText(SIX_MONTHS_AGO, testUser(language = "ru"), localization)

        assertThat(text).isEqualTo(localization.t("finance.history.premium_required", "ru", FREE_HISTORY_DAYS))
    }

    @Test
    fun `lets free users see the period inside their history`() {
        assertThat(limits.premiumRequiredText(TODAY.withDayOfMonth(1), testUser(), localization)).isNull()
    }

    @Test
    fun `lets premium users see any period`() {
        val premium = testUser().copy(subscriptionTier = SubscriptionTier.PAID, subscriptionExpiresAt = LocalDateTime.now().plusDays(1))

        assertThat(limits.premiumRequiredText(SIX_MONTHS_AGO, premium, localization)).isNull()
    }
}
