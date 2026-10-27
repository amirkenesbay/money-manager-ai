package ai.moneymanager.domain.model

import java.math.BigDecimal

enum class SubscriptionTier {
    FREE,
    PAID
}

const val PREMIUM_SUBSCRIBE_LINK = "https://t.me/tribute/app?startapp=s11rb"
val PREMIUM_MONTHLY_PRICE: BigDecimal = BigDecimal("990")
val PREMIUM_PRICE_CURRENCY: Currency = Currency.KZT
