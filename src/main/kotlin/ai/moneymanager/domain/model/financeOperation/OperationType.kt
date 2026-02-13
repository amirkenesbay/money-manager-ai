package ai.moneymanager.domain.model.financeOperation

enum class OperationType (val financeCategoryName: String){
    EXPENSE("expense"),
    INCOME("income");

    companion object {
        fun from(financeCategoryName: String): OperationType? =
            entries.firstOrNull { it.financeCategoryName.equals(financeCategoryName, ignoreCase = true) }
    }
}