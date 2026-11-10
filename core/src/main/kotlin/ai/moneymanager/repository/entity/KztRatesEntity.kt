package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.Currency
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate

@Document(collection = "kzt_rates")
data class KztRatesEntity(
    @Id
    val day: String,
    val kztPerUnit: Map<Currency, BigDecimal>,
) {
    val date: LocalDate get() = LocalDate.parse(day)

    companion object {
        fun dayOf(date: LocalDate): String = date.toString()
    }
}
