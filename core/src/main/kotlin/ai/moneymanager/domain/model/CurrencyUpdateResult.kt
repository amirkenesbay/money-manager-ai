package ai.moneymanager.domain.model

sealed interface CurrencyUpdateResult {
    data class Updated(val group: MoneyGroup) : CurrencyUpdateResult
    data object RateUnavailable : CurrencyUpdateResult
    data object NotAllowed : CurrencyUpdateResult
}
