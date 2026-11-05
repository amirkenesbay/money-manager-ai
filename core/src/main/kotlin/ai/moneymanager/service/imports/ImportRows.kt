package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.imports.ColumnMapping
import ai.moneymanager.domain.model.imports.ColumnRole
import ai.moneymanager.domain.model.imports.ImportRow
import ai.moneymanager.domain.model.imports.ImportTable
import ai.moneymanager.domain.model.imports.SkipReason

const val FALLBACK_CATEGORY = "Другое"

data class ParsedRows(val rows: List<ImportRow>, val skipped: Map<SkipReason, Int>)

private sealed interface RowOutcome {
    data class Parsed(val row: ImportRow) : RowOutcome
    data class Skipped(val reason: SkipReason) : RowOutcome
}

private class RowReader(private val table: ImportTable, private val mapping: ColumnMapping) {
    fun text(row: List<String>, role: ColumnRole): String = table.cell(row, mapping[role])
}

fun parseRows(table: ImportTable, mapping: ColumnMapping, currency: Currency): ParsedRows {
    val reader = RowReader(table, mapping)
    val signed = mapping[ColumnRole.AMOUNT] != null && table.rows.any { parseAmount(reader.text(it, ColumnRole.AMOUNT))?.signum() == -1 }
    val outcomes = table.rows.map { rowOf(it, reader, currency, signed) }
    return ParsedRows(
        rows = outcomes.filterIsInstance<RowOutcome.Parsed>().map { it.row },
        skipped = outcomes.filterIsInstance<RowOutcome.Skipped>().groupingBy { it.reason }.eachCount(),
    )
}

private fun rowOf(row: List<String>, reader: RowReader, currency: Currency, signed: Boolean): RowOutcome {
    val date = parseDate(reader.text(row, ColumnRole.DATE)) ?: return RowOutcome.Skipped(SkipReason.INVALID)
    if (!matchesCurrency(reader.text(row, ColumnRole.CURRENCY), currency)) return RowOutcome.Skipped(SkipReason.CURRENCY)
    val typeWord = typeWordOf(reader.text(row, ColumnRole.TYPE))
    if (typeWord == TypeWord.TRANSFER) return RowOutcome.Skipped(SkipReason.TRANSFER)
    val expense = parseAmount(reader.text(row, ColumnRole.EXPENSE))?.abs()?.takeIf { it.signum() > 0 }
    val income = parseAmount(reader.text(row, ColumnRole.INCOME))?.abs()?.takeIf { it.signum() > 0 }
    if (expense != null && income != null) return RowOutcome.Skipped(SkipReason.TRANSFER)
    val (type, amount) = when {
        expense != null -> CategoryType.EXPENSE to expense
        income != null -> CategoryType.INCOME to income
        else -> {
            val value = parseAmount(reader.text(row, ColumnRole.AMOUNT))?.takeIf { it.signum() != 0 } ?: return RowOutcome.Skipped(SkipReason.INVALID)
            typeOf(typeWord, value.signum(), signed) to value.abs()
        }
    }
    val incomeCategory = reader.text(row, ColumnRole.INCOME_CATEGORY).takeIf { type == CategoryType.INCOME && it.isNotEmpty() }
    val category = incomeCategory ?: reader.text(row, ColumnRole.CATEGORY).ifEmpty { FALLBACK_CATEGORY }
    return RowOutcome.Parsed(ImportRow(type, amount, date, category, reader.text(row, ColumnRole.COMMENT).ifEmpty { null }))
}

private fun typeOf(word: TypeWord?, sign: Int, signed: Boolean): CategoryType = when {
    word == TypeWord.INCOME -> CategoryType.INCOME
    word == TypeWord.EXPENSE -> CategoryType.EXPENSE
    signed && sign > 0 -> CategoryType.INCOME
    else -> CategoryType.EXPENSE
}
