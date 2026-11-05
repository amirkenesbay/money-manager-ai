package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DATE_FORMATS = listOf("yyyy-MM-dd", "dd.MM.yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yy", "yyyy/MM/dd", "d.M.yyyy", "M/d/yyyy")
    .map { DateTimeFormatter.ofPattern(it) }
private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)
private val EXCEL_SERIALS = 20_000..80_000
private val DATE_TIME_SEPARATORS = charArrayOf(' ', 'T')
private val AMOUNT_NOISE = Regex("[\\s\\u00A0'₸₽$€a-zA-Zа-яА-Я]")
private const val DECIMAL_DIGITS_MAX = 2
private const val MINUS_SIGNS = "-−–"

private val INCOME_WORDS = setOf("income", "доход", "кіріс", "поступление", "приход", "зачисление")
private val EXPENSE_WORDS = setOf("expense", "exp", "exp.", "расход", "шығыс", "списание", "трата")
private val TRANSFER_WORDS = setOf("transfer", "перевод", "аударым", "transfer-out", "transfer-in", "перемещение")

private val CURRENCY_ALIASES: Map<Currency, Set<String>> = mapOf(
    Currency.KZT to setOf("kzt", "₸", "тенге", "тг", "теңге"),
    Currency.RUB to setOf("rub", "rur", "₽", "руб", "руб.", "рубль"),
    Currency.USD to setOf("usd", "$", "доллар"),
    Currency.EUR to setOf("eur", "€", "евро"),
    Currency.KGS to setOf("kgs", "сом", "с"),
)

enum class TypeWord {
    EXPENSE,
    INCOME,
    TRANSFER,
}

fun parseDate(raw: String): LocalDate? {
    val value = raw.trim().split(*DATE_TIME_SEPARATORS).first()
    if (value.isEmpty()) return null
    value.toDoubleOrNull()?.toInt()?.takeIf { it in EXCEL_SERIALS }?.let { return EXCEL_EPOCH.plusDays(it.toLong()) }
    return DATE_FORMATS.firstNotNullOfOrNull { format ->
        try {
            LocalDate.parse(value, format)
        } catch (exception: DateTimeParseException) {
            null
        }
    }
}

fun parseAmount(raw: String): BigDecimal? {
    val cleaned = raw.replace(AMOUNT_NOISE, "").let { if (it.firstOrNull()?.let(MINUS_SIGNS::contains) == true) "-" + it.drop(1) else it }
    if (cleaned.isEmpty() || cleaned == "-") return null
    val lastComma = cleaned.lastIndexOf(',')
    val lastDot = cleaned.lastIndexOf('.')
    val normalized = when {
        lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) cleaned.replace(".", "").replace(',', '.') else cleaned.replace(",", "")
        lastComma >= 0 -> if (cleaned.length - lastComma - 1 in 1..DECIMAL_DIGITS_MAX) cleaned.replace(',', '.') else cleaned.replace(",", "")
        else -> cleaned
    }
    return normalized.toBigDecimalOrNull()
}

fun typeWordOf(raw: String): TypeWord? {
    val value = raw.trim().lowercase()
    return when {
        value.isEmpty() -> null
        TRANSFER_WORDS.any { value.startsWith(it) } -> TypeWord.TRANSFER
        INCOME_WORDS.any { value.startsWith(it) } -> TypeWord.INCOME
        EXPENSE_WORDS.any { value.startsWith(it) } -> TypeWord.EXPENSE
        else -> null
    }
}

fun matchesCurrency(raw: String, currency: Currency): Boolean {
    val value = raw.trim().lowercase()
    if (value.isEmpty()) return true
    return value == currency.name.lowercase() || value in CURRENCY_ALIASES[currency].orEmpty()
}
