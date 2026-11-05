package ai.moneymanager.domain.model.imports

import ai.moneymanager.domain.model.CategoryType
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

enum class ColumnRole {
    DATE,
    AMOUNT,
    EXPENSE,
    INCOME,
    TYPE,
    CATEGORY,
    INCOME_CATEGORY,
    COMMENT,
    CURRENCY,
}

enum class ImportSource {
    ZENMONEY,
    COINKEEPER,
    MONEY_MANAGER,
    DETECTED,
    AI,
    MANUAL,
}

enum class SkipReason {
    TRANSFER,
    CURRENCY,
    INVALID,
    DUPLICATE,
}

data class ImportTable(
    val headers: List<String>,
    val rows: List<List<String>>,
) {
    fun cell(row: List<String>, index: Int?): String = index?.let { row.getOrNull(it) }?.trim().orEmpty()
}

data class ColumnMapping(
    val roles: Map<ColumnRole, Int>,
) {
    operator fun get(role: ColumnRole): Int? = roles[role]

    val complete: Boolean
        get() = ColumnRole.DATE in roles && (ColumnRole.AMOUNT in roles || ColumnRole.EXPENSE in roles || ColumnRole.INCOME in roles)
}

data class ImportRow(
    val type: CategoryType,
    val amount: BigDecimal,
    val date: LocalDate,
    val category: String,
    val comment: String?,
) {
    val categoryKey: String
        get() = categoryKeyOf(type, category)
}

fun categoryKeyOf(type: CategoryType, name: String): String = "${type.name}:${name.trim().lowercase()}"

data class ImportColumn(
    val header: String,
    val samples: List<String>,
    val role: ColumnRole?,
)

data class ImportCategory(
    val key: String,
    val name: String,
    val type: CategoryType,
    val count: Int,
    val existingId: ObjectId?,
)

data class ImportPreview(
    val token: String,
    val source: ImportSource?,
    val columns: List<ImportColumn>,
    val count: Int,
    val from: LocalDate?,
    val to: LocalDate?,
    val skipped: Map<SkipReason, Int>,
    val categories: List<ImportCategory>,
)

sealed interface CategoryDecision {
    data object Create : CategoryDecision
    data object Skip : CategoryDecision
    data class Existing(val categoryId: ObjectId) : CategoryDecision
}

data class ImportResult(
    val importId: ObjectId,
    val count: Int,
    val createdCategories: Int,
    val from: LocalDate?,
    val to: LocalDate?,
)
