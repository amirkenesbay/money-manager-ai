package ai.moneymanager.domain.model.nlp.enum

enum class CurrencyType(val currency: String) {
    KZT("kzt");

    companion object {
        fun from(name: String): CurrencyType? =
            entries.firstOrNull { it.currency == name }
    }
}