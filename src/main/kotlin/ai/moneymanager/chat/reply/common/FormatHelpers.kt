package ai.moneymanager.chat.reply.common

import ai.moneymanager.domain.model.BalanceBreakdown
import ai.moneymanager.domain.model.Category
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

fun formatAmount(amount: BigDecimal): String = "${amountFormat.format(amount)}$CURRENCY_SYMBOL"

/** Telegram HTML parse mode: жирный текст. Использовать только на экранах с parseMode = HTML. */
fun bold(text: String): String = "<b>$text</b>"

fun italic(text: String): String = "<i>$text</i>"

/** Моноширинная строка — колонки цифр и баров не пляшут. */
fun code(text: String): String = "<code>$text</code>"

/** Моноширинный блок: настоящее табличное выравнивание для бар-чартов. */
fun pre(text: String): String = "<pre>$text</pre>"

/** Блок с вертикальной чертой — визуально выделенные итоги. */
fun blockquote(text: String): String = "<blockquote>$text</blockquote>"

/** Сворачиваемый блок — длинные списки не заваливают экран. */
fun expandableBlockquote(text: String): String = "<blockquote expandable>$text</blockquote>"

fun link(label: String, url: String): String = "<a href=\"$url\">$label</a>"

private const val BAR_WIDTH = 12
private const val BAR_FILLED = "█"
private const val BAR_EMPTY = "░"

/** Горизонтальный бар: доля value от max, ширина фиксированная — не переносится. */
fun progressBar(value: BigDecimal, max: BigDecimal, width: Int = BAR_WIDTH): String {
    val filled = if (max > BigDecimal.ZERO) {
        value.multiply(BigDecimal.valueOf(width.toLong()))
            .divide(max, 0, java.math.RoundingMode.HALF_UP)
            .toInt()
            .coerceIn(0, width)
    } else 0
    return BAR_FILLED.repeat(filled) + BAR_EMPTY.repeat(width - filled)
}

/** Экранирование пользовательского текста для экранов с parseMode = HTML. */
fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

fun formatTime(hour: Int, minute: Int): String = TIME_PATTERN.format(hour, minute)

fun formatIconPrefix(icon: String?): String =
    icon?.takeIf { it.isNotBlank() }?.let { "$it$ICON_NAME_SEPARATOR" } ?: ""

fun formatDescriptionSuffix(description: String?): String =
    description?.takeIf { it.isNotBlank() }?.let { "$DESCRIPTION_PREFIX$it$DESCRIPTION_SUFFIX" } ?: ""

/** Текст кнопки операции в списке для редактирования — общий для reply-экрана и transition-матчинга по buttonText. */
fun operationListButtonText(date: String, icon: String?, categoryName: String, signedAmount: String): String =
    "$date ${icon ?: DEFAULT_CATEGORY_ICON} $categoryName $signedAmount"

/** Текст кнопки категории («иконка имя») — общий для reply-экранов и transition-матчинга по buttonText. */
fun categoryButtonText(category: Category): String =
    "${category.icon ?: DEFAULT_CATEGORY_ICON} ${category.name}"

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
    |${bold(localizationService.t("balance.view.title", language))}
    |
    |${localizationService.t("balance.view.initial", language, formatAmount(balance.initial))}
    |${localizationService.t("balance.view.income", language, formatAmount(balance.income))}
    |${localizationService.t("balance.view.expense", language, formatAmount(balance.expense))}
    |
    |${blockquote(bold(localizationService.t("balance.view.total", language, formatAmount(balance.total))))}
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
