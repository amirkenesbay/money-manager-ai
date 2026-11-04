package ai.moneymanager.domain.model

import java.math.BigDecimal
import java.time.LocalDate

enum class ExportFormat(val extension: String) {
    CSV("csv"),
    XLSX("xlsx"),
}

data class ExportRow(
    val date: LocalDate,
    val type: String,
    val category: String,
    val amount: BigDecimal,
    val currency: String,
    val description: String,
    val author: String,
)

data class ExportTable(
    val headers: List<String>,
    val rows: List<ExportRow>,
)
