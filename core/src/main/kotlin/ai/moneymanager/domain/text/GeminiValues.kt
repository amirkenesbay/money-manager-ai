package ai.moneymanager.domain.text

private const val NULL_LITERAL = "null"

fun String?.geminiValue(): String? = this?.trim()?.takeIf { it.isNotEmpty() && !it.equals(NULL_LITERAL, ignoreCase = true) }
