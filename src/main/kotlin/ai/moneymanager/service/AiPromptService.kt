package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val SYSTEM_PROMPT_PATH = "prompts/ai-system-prompt.md"
private const val VOICE_TRANSCRIPTION_PROMPT_PATH = "prompts/ai-voice-transcription.md"
private const val OUT_OF_CONTEXT_PROMPT_PATH = "prompts/ai-out-of-context.md"

private const val PLACEHOLDER_USER_MESSAGE = "{userMessage}"
private const val PLACEHOLDER_REPLY_LANGUAGE = "{replyLanguage}"

private const val CURRENT_DATE_TEMPLATE = "Today is %s (%s)."

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

@Service
class AiPromptService {

    val systemPrompt: String by lazy { loadPrompt(SYSTEM_PROMPT_PATH) }

    val voiceTranscriptionPrompt: String by lazy { loadPrompt(VOICE_TRANSCRIPTION_PROMPT_PATH) }

    private val outOfContextTemplate: String by lazy { loadPrompt(OUT_OF_CONTEXT_PROMPT_PATH) }

    fun outOfContextPrompt(userMessage: String, replyLanguage: String): String =
        outOfContextTemplate
            .replace(PLACEHOLDER_USER_MESSAGE, userMessage)
            .replace(PLACEHOLDER_REPLY_LANGUAGE, replyLanguage)

    fun currentDatePreamble(): String {
        val today = LocalDate.now()
        val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return CURRENT_DATE_TEMPLATE.format(today, weekday)
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
