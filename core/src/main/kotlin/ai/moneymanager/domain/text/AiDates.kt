package ai.moneymanager.domain.text

import java.time.LocalDate

fun parseIsoDateOrNull(raw: String?): LocalDate? =
    raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun resolveOperationDate(raw: String?, today: LocalDate = LocalDate.now()): LocalDate {
    val parsed = parseIsoDateOrNull(raw) ?: return today
    return if (parsed.isAfter(today)) today else parsed
}
