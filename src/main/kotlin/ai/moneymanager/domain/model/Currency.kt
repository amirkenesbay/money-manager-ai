package ai.moneymanager.domain.model

enum class Currency(val symbol: String) {
    KZT("₸"),
    USD("$"),
    RUB("₽"),
    EUR("€"),
    KGS("с");

    companion object {
        val DEFAULT: Currency = KZT
    }
}
