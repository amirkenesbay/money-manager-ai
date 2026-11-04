package ai.moneymanager.domain.text

import ai.moneymanager.repository.entity.FinanceOperationEntity
import java.math.BigDecimal

private const val TAG_MARK = '#'
private val TAG = Regex("#([\\p{L}\\p{N}_]+)")
private val AMOUNT = Regex("^[\\d\\s]+([.,]\\d{1,2})?$")
private val SPACES = Regex("\\s+")
private const val DECIMAL_COMMA = ','
private const val DECIMAL_POINT = '.'

fun tagsOf(description: String?): List<String> =
    description?.let { text -> TAG.findAll(text).map { it.groupValues[1].lowercase() }.distinct().toList() }.orEmpty()

private fun hasTag(operation: FinanceOperationEntity, tag: String): Boolean = tag.removePrefix(TAG_MARK.toString()) in tagsOf(operation.description)

fun matchesSearch(operation: FinanceOperationEntity, query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    if (needle.first() == TAG_MARK) return hasTag(operation, needle)
    return amountOf(needle)?.let { operation.amount.compareTo(it) == 0 } == true ||
        operation.description?.lowercase()?.contains(needle) == true ||
        operation.categoryName.lowercase().contains(needle)
}

private fun amountOf(query: String): BigDecimal? =
    query.takeIf { AMOUNT.matches(it) }?.replace(SPACES, "")?.replace(DECIMAL_COMMA, DECIMAL_POINT)?.toBigDecimalOrNull()
