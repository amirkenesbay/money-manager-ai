package ai.moneymanager.chat.transition.ai

import java.time.LocalDate

fun parseIsoDateOrNull(raw: String?): LocalDate? =
    raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Дата операции из ответа Gemini: невалидная или будущая → сегодня. */
fun resolveOperationDate(raw: String?): LocalDate {
    val today = LocalDate.now()
    val parsed = parseIsoDateOrNull(raw) ?: return today
    return if (parsed.isAfter(today)) today else parsed
}
