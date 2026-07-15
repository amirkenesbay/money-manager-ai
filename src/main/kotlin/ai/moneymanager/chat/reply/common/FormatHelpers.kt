package ai.moneymanager.chat.reply.common

import ai.moneymanager.domain.model.BalanceBreakdown
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
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
private const val ICON_NAME_SEPARATOR = " "
private const val DESCRIPTION_PREFIX = " ("
private const val DESCRIPTION_SUFFIX = ")"
private const val TIME_PATTERN = "%02d:%02d"

const val DEFAULT_CATEGORY_ICON: String = "📌"

private const val BOT_USERNAME = "moneyManagerAIbot"
private const val INVITE_LINK_TEMPLATE = "https://t.me/%s?start=join_%s"

fun buildInviteLink(inviteToken: String): String = INVITE_LINK_TEMPLATE.format(BOT_USERNAME, inviteToken)

private val amountFormat = DecimalFormat(
    AMOUNT_PATTERN,
    DecimalFormatSymbols().apply { groupingSeparator = THOUSANDS_SEPARATOR }
)

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(SHORT_DATE_PATTERN)

const val SECTION_SEPARATOR: String = "\n" + SECTION_RULE
const val SECTION_SEPARATOR_WITH_BLANK_LINE: String = "\n\n" + SECTION_RULE

fun formatAmount(amount: BigDecimal): String = "${amountFormat.format(amount)}$CURRENCY_SYMBOL"

fun formatTime(hour: Int, minute: Int): String = TIME_PATTERN.format(hour, minute)

fun formatIconPrefix(icon: String?): String =
    icon?.takeIf { it.isNotBlank() }?.let { "$it$ICON_NAME_SEPARATOR" } ?: ""

fun formatDescriptionSuffix(description: String?): String =
    description?.takeIf { it.isNotBlank() }?.let { "$DESCRIPTION_PREFIX$it$DESCRIPTION_SUFFIX" } ?: ""

fun formatSignedAmount(type: CategoryType, amount: BigDecimal): String {
    val sign = when (type) {
        CategoryType.INCOME -> POSITIVE_SIGN
        CategoryType.EXPENSE -> NEGATIVE_SIGN
    }
    return "$sign${formatAmount(amount)}"
}

fun formatBalanceBreakdown(
    balance: BalanceBreakdown,
    localizationService: LocalizationService,
    language: String?
): String = """
    |${localizationService.t("balance.view.title", language)}
    |
    |${localizationService.t("balance.view.initial", language, formatAmount(balance.initial))}
    |${localizationService.t("balance.view.income", language, formatAmount(balance.income))}
    |${localizationService.t("balance.view.expense", language, formatAmount(balance.expense))}
    |
    |${localizationService.t("balance.view.total", language, formatAmount(balance.total))}
""".trimMargin()

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
