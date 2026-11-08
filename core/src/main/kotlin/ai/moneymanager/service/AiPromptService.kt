package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryRule
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.service.question.askFinanceCallOf
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val SYSTEM_PROMPT_PATH = "prompts/ai-system-prompt.md"
private const val TOOL_DOCS_PATH = "prompts/ai-tools.json"
private const val VOICE_TRANSCRIPTION_PROMPT_PATH = "prompts/ai-voice-transcription.md"
private const val OUT_OF_CONTEXT_PROMPT_PATH = "prompts/ai-out-of-context.md"
private const val IMPORT_COLUMNS_PROMPT_PATH = "prompts/ai-import-columns.md"
private const val PLACEHOLDER_TABLE = "{table}"

private const val PLACEHOLDER_USER_MESSAGE = "{userMessage}"
private const val PLACEHOLDER_REPLY_LANGUAGE = "{replyLanguage}"

private const val CURRENT_DATE_TEMPLATE = "Today is %s (%s)."
private const val LANGUAGE_TEMPLATE = "The user's app language is %s. Voice messages are most likely spoken in it."
private const val CURRENCY_TEMPLATE = "The group's currency is %s. Assume all amounts are in this currency; do not mention or convert to any other currency."

private const val CATEGORY_CONTEXT_HEADER =
    "User's active categories for the current group (use the EXACT name when a semantically applicable one exists; do NOT propose a new one in that case):"
private const val CATEGORY_CONTEXT_EMPTY_LINE =
    "User has no categories yet — propose a generalized name and emoji for any new financial operation."
private const val CATEGORY_LINE_EXPENSE_PREFIX = "EXPENSE: "
private const val CATEGORY_LINE_INCOME_PREFIX = "INCOME:  "
private const val CATEGORY_LINE_NONE = "(none)"
private const val CATEGORY_ITEMS_SEPARATOR = ", "
private const val CATEGORY_LIST_OVERFLOW_SUFFIX = ", …"
private const val MAX_CATEGORIES_IN_PROMPT = 50

private const val REMEMBERED_PLACES_HEADER =
    "Places the user has corrected before (when the description names one of these places, use its category):"
private const val REMEMBERED_PLACE_TEMPLATE = "%s → %s"
private const val MAX_REMEMBERED_PLACES_IN_PROMPT = 20

private const val SHARED_MEMBERS_TEMPLATE = "Members who share expenses in this group (the speaker is ME = %s): %s"
private const val MEMBER_SEPARATOR = ", "

private const val PREVIOUS_QUESTION_TEMPLATE =
    "Previous question of this user (a short follow-up like «а в сентябре?» repeats it with only the mentioned parameter changed): %s"

data class SharedMembers(val speaker: String, val names: List<String>)

@Service
class AiPromptService {
    val systemPrompt: String by lazy { loadPrompt(SYSTEM_PROMPT_PATH) }

    val toolDocsJson: String by lazy { loadPrompt(TOOL_DOCS_PATH) }

    val voiceTranscriptionPrompt: String by lazy { loadPrompt(VOICE_TRANSCRIPTION_PROMPT_PATH) }

    private val importColumnsTemplate: String by lazy { loadPrompt(IMPORT_COLUMNS_PROMPT_PATH) }

    fun importColumnsPrompt(table: String): String = importColumnsTemplate.replace(PLACEHOLDER_TABLE, table)

    private val outOfContextTemplate: String by lazy { loadPrompt(OUT_OF_CONTEXT_PROMPT_PATH) }

    fun outOfContextPrompt(userMessage: String, replyLanguage: String): String =
        outOfContextTemplate
            .replace(PLACEHOLDER_USER_MESSAGE, userMessage)
            .replace(PLACEHOLDER_REPLY_LANGUAGE, replyLanguage)

    fun currentDatePreamble(today: LocalDate): String {
        val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return CURRENT_DATE_TEMPLATE.format(today, weekday)
    }

    fun currencyPreamble(currency: Currency): String = CURRENCY_TEMPLATE.format(currency.name)

    fun groupContext(
        today: LocalDate,
        currency: Currency,
        categories: List<Category>,
        rules: List<CategoryRule> = emptyList(),
        previousQuestion: FinanceQuestion? = null,
        sharedMembers: SharedMembers? = null,
        language: String? = null,
    ): String =
        listOf(
            currentDatePreamble(today),
            language?.let { LANGUAGE_TEMPLATE.format(Locale.forLanguageTag(it).getDisplayLanguage(Locale.ENGLISH)) }.orEmpty(),
            currencyPreamble(currency),
            categoryContextPreamble(categories),
            rememberedPlacesPreamble(categories, rules),
            previousQuestion?.let { PREVIOUS_QUESTION_TEMPLATE.format(askFinanceCallOf(it)) }.orEmpty(),
            sharedMembers?.let { SHARED_MEMBERS_TEMPLATE.format(it.speaker, it.names.joinToString(MEMBER_SEPARATOR)) }.orEmpty(),
        )
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    fun rememberedPlacesPreamble(categories: List<Category>, rules: List<CategoryRule>): String {
        val namesById = categories.associate { it.id to it.name }
        val lines = rules
            .mapNotNull { rule -> namesById[rule.categoryId]?.let { REMEMBERED_PLACE_TEMPLATE.format(rule.merchantKey, it) } }
            .take(MAX_REMEMBERED_PLACES_IN_PROMPT)
        if (lines.isEmpty()) return ""
        return (listOf(REMEMBERED_PLACES_HEADER) + lines).joinToString("\n")
    }

    fun categoryContextPreamble(categories: List<Category>): String {
        if (categories.isEmpty()) {
            return "$CATEGORY_CONTEXT_HEADER\n$CATEGORY_CONTEXT_EMPTY_LINE"
        }
        val expenseLine = formatCategoryLine(categories, CategoryType.EXPENSE)
        val incomeLine = formatCategoryLine(categories, CategoryType.INCOME)
        return buildString {
            append(CATEGORY_CONTEXT_HEADER).append('\n')
            append(CATEGORY_LINE_EXPENSE_PREFIX).append(expenseLine).append('\n')
            append(CATEGORY_LINE_INCOME_PREFIX).append(incomeLine)
        }
    }

    private fun formatCategoryLine(categories: List<Category>, type: CategoryType): String {
        val typed = categories.filter { it.type == type }
        if (typed.isEmpty()) return CATEGORY_LINE_NONE
        val limited = typed.take(MAX_CATEGORIES_IN_PROMPT)
        val joined = limited.joinToString(CATEGORY_ITEMS_SEPARATOR) { it.name }
        return if (typed.size > MAX_CATEGORIES_IN_PROMPT) joined + CATEGORY_LIST_OVERFLOW_SUFFIX else joined
    }

    private fun loadPrompt(path: String): String =
        ClassPathResource(path).inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8).trim() }
}
