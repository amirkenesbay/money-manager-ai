package ai.moneymanager.domain.model

import java.math.BigDecimal

enum class SubscriptionTier {
    FREE,
    PAID
}

enum class SubscriptionPeriod {
    MONTH,
    QUARTER,
    YEAR,
}

data class PremiumPrice(val period: SubscriptionPeriod, val amount: BigDecimal, val currency: Currency)

const val PREMIUM_SUBSCRIBE_LINK = "https://t.me/tribute/app?startapp=s11rb"

val PREMIUM_PRICES: List<PremiumPrice> = listOf(
    PremiumPrice(SubscriptionPeriod.MONTH, BigDecimal("4.41"), Currency.USD),
    PremiumPrice(SubscriptionPeriod.YEAR, BigDecimal("49.69"), Currency.USD),
)
