package ai.moneymanager.domain.model.financeOperation

enum class FinanceCategory (val financeCategoryName: String){
    EXPENSE("expense"),
    INCOME("income");

    companion object {
        fun from(financeCategoryName: String): FinanceCategory? =
            entries.firstOrNull { it.financeCategoryName.equals(financeCategoryName, ignoreCase = true) }
    }
}