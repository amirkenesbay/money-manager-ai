package ai.moneymanager.chat.reply.common

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.UserInfo
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter

private const val AMOUNT_PATTERN = "#,##0"
private const val THOUSANDS_SEPARATOR = ' '
private const val CURRENCY_SYMBOL = "₸"
private const val DATE_PATTERN = "dd.MM.yyyy"
private const val SHORT_DATE_PATTERN = "dd.MM"
private const val SECTION_RULE = "━━━━━━━━━━━━━━━━━━━━━"
private const val POSITIVE_SIGN = "+"
private const val NEGATIVE_SIGN = "−"

const val DEFAULT_CATEGORY_ICON: String = "📌"

private val amountFormat = DecimalFormat(
    AMOUNT_PATTERN,
    DecimalFormatSymbols().apply { groupingSeparator = THOUSANDS_SEPARATOR }
)

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(SHORT_DATE_PATTERN)

const val SECTION_SEPARATOR: String = "\n" + SECTION_RULE
const val SECTION_SEPARATOR_WITH_BLANK_LINE: String = "\n\n" + SECTION_RULE

fun formatAmount(amount: BigDecimal): String = "${amountFormat.format(amount)}$CURRENCY_SYMBOL"

fun formatSignedAmount(type: CategoryType, amount: BigDecimal): String {
    val sign = when (type) {
        CategoryType.INCOME -> POSITIVE_SIGN
        CategoryType.EXPENSE -> NEGATIVE_SIGN
    }
    return "$sign${formatAmount(amount)}"
}

fun formatUserDisplayName(userInfo: UserInfo?, fallbackId: Long): String {
    if (userInfo == null) return "ID $fallbackId"

    val fullName = listOfNotNull(userInfo.firstName, userInfo.lastName)
        .joinToString(" ")

    return when {
        fullName.isNotEmpty() -> fullName
        !userInfo.username.isNullOrEmpty() -> "@${userInfo.username}"
        else -> "ID $fallbackId"
    }
}

fun categoryTypeLabel(type: CategoryType?, form: CategoryTypeForm = CategoryTypeForm.GENITIVE): String {
    return when (form) {
        CategoryTypeForm.GENITIVE -> when (type) {
            CategoryType.EXPENSE -> "расхода"
            CategoryType.INCOME -> "дохода"
            else -> "категории"
        }
        CategoryTypeForm.GENITIVE_PLURAL -> when (type) {
            CategoryType.EXPENSE -> "расходов"
            CategoryType.INCOME -> "доходов"
            else -> ""
        }
    }
}

enum class CategoryTypeForm {
    GENITIVE,
    GENITIVE_PLURAL
}
