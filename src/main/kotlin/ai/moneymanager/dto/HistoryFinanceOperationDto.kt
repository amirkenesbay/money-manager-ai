    package ai.moneymanager.dto

    import ai.moneymanager.domain.model.financeOperation.CurrencyType
    //import ai.moneymanager.domain.model.financeOperation.DayType
    import java.math.BigDecimal
    import java.time.LocalDateTime

    data class HistoryFinanceOperationDto (
        val telegramUserId: Long,
        val amount: BigDecimal,
        val currency: CurrencyType,
        //val day: DayType,
        val description: String?,
        val createdAt: LocalDateTime?
        )