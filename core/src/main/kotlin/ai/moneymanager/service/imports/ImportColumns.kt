package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.imports.ColumnMapping
import ai.moneymanager.domain.model.imports.ColumnRole
import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.domain.model.imports.ImportTable

private const val SAMPLE_ROWS = 50
private const val VALUE_SHARE = 0.8

private data class KnownFormat(val source: ImportSource, val markers: Set<String>, val columns: Map<ColumnRole, List<String>>)

private val KNOWN_FORMATS = listOf(
    KnownFormat(
        ImportSource.ZENMONEY,
        setOf("outcomeaccountname", "incomeaccountname"),
        mapOf(
            ColumnRole.DATE to listOf("date"), ColumnRole.CATEGORY to listOf("categoryname"), ColumnRole.COMMENT to listOf("comment", "payee"),
            ColumnRole.EXPENSE to listOf("outcome"), ColumnRole.INCOME to listOf("income"), ColumnRole.CURRENCY to listOf("outcomecurrencyshorttitle"),
        ),
    ),
    KnownFormat(
        ImportSource.COINKEEPER,
        setOf("тип", "из", "в", "сумма"),
        mapOf(
            ColumnRole.DATE to listOf("дата", "данные"), ColumnRole.TYPE to listOf("тип"), ColumnRole.INCOME_CATEGORY to listOf("из"),
            ColumnRole.CATEGORY to listOf("в"), ColumnRole.AMOUNT to listOf("сумма"), ColumnRole.CURRENCY to listOf("валюта"),
            ColumnRole.COMMENT to listOf("примечание", "метки"),
        ),
    ),
    KnownFormat(
        ImportSource.MONEY_MANAGER,
        setOf("income/expense"),
        mapOf(
            ColumnRole.DATE to listOf("period", "date"), ColumnRole.CATEGORY to listOf("category"), ColumnRole.COMMENT to listOf("note", "description"),
            ColumnRole.AMOUNT to listOf("amount"), ColumnRole.TYPE to listOf("income/expense"), ColumnRole.CURRENCY to listOf("currency"),
        ),
    ),
)

private val HEADER_WORDS: Map<ColumnRole, List<String>> = linkedMapOf(
    ColumnRole.DATE to listOf("date", "дата", "күні", "күн", "день", "period", "время"),
    ColumnRole.TYPE to listOf("type", "тип", "income/expense", "вид"),
    ColumnRole.EXPENSE to listOf("outcome", "expense", "расход", "списание", "debit", "шығыс"),
    ColumnRole.INCOME to listOf("income", "доход", "поступление", "credit", "кіріс"),
    ColumnRole.AMOUNT to listOf("amount", "sum", "сумма", "сомасы", "value"),
    ColumnRole.CATEGORY to listOf("category", "категория", "санат", "статья"),
    ColumnRole.COMMENT to listOf("comment", "note", "примечание", "комментарий", "описание", "description", "memo", "payee"),
    ColumnRole.CURRENCY to listOf("currency", "валюта"),
)

fun knownMapping(table: ImportTable): Pair<ImportSource, ColumnMapping>? {
    val headers = table.headers.map { it.trim().lowercase() }
    val format = KNOWN_FORMATS.firstOrNull { headers.containsAll(it.markers) } ?: return null
    val roles = format.columns.mapNotNull { (role, candidates) ->
        candidates.firstNotNullOfOrNull { candidate -> headers.indexOf(candidate).takeIf { it >= 0 } }?.let { role to it }
    }.toMap()
    return ColumnMapping(roles).takeIf { it.complete }?.let { format.source to it }
}

fun guessMapping(table: ImportTable): ColumnMapping? {
    val headers = table.headers.map { it.trim().lowercase() }
    val roles = mutableMapOf<ColumnRole, Int>()
    HEADER_WORDS.forEach { (role, words) ->
        val index = headers.indices.firstOrNull { index -> index !in roles.values && words.any { headers[index] == it || headers[index].startsWith(it) } }
        if (index != null) roles[role] = index
    }
    if (ColumnRole.DATE !in roles) columnWhere(table, roles.values) { parseDate(it) != null }?.let { roles[ColumnRole.DATE] = it }
    if (listOf(ColumnRole.AMOUNT, ColumnRole.EXPENSE, ColumnRole.INCOME).none { it in roles }) {
        columnWhere(table, roles.values) { parseAmount(it) != null }?.let { roles[ColumnRole.AMOUNT] = it }
    }
    return ColumnMapping(roles).takeIf { it.complete }
}

private fun columnWhere(table: ImportTable, taken: Collection<Int>, matches: (String) -> Boolean): Int? {
    val sample = table.rows.take(SAMPLE_ROWS)
    return table.headers.indices.firstOrNull { index ->
        index !in taken && sample.count { matches(table.cell(it, index)) } >= sample.size * VALUE_SHARE
    }
}
