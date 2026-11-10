package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryRule
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.ForeignAmount
import ai.moneymanager.domain.model.Recurrence
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.OperationSuggestion
import ai.moneymanager.domain.text.extractLeadingNonLetters
import ai.moneymanager.domain.text.matchesEntityName
import ai.moneymanager.domain.text.merchantKey
import ai.moneymanager.domain.text.resolveOperationDate
import ai.moneymanager.domain.text.stripLeadingNonLetters
import java.time.LocalDate

private data class OperationCommand(
    val type: CategoryType,
    val amount: Double,
    val category: String?,
    val description: String?,
    val suggestedIcon: String?,
    val operationDate: String?,
)

fun suggestOperation(
    command: BotCommand,
    categories: List<Category>,
    today: LocalDate = LocalDate.now(),
    rules: List<CategoryRule> = emptyList(),
): OperationSuggestion? {
    val operation = operationOf(command) ?: return null
    if (operation.amount <= 0) return OperationSuggestion.AmountNotPositive
    val description = operation.description?.trim()?.takeIf { it.isNotEmpty() }
    val date = resolveOperationDate(operation.operationDate, today)
    learnedCategory(description, operation.type, categories, rules)?.let {
        return OperationSuggestion.Ready(operation.type, operation.amount, it, description, date)
    }
    val missing = OperationSuggestion.CategoryMissing(operation.type, operation.amount, description, date)
    val rawCategory = operation.category?.takeIf { it.isNotBlank() } ?: return missing
    val cleanCategory = stripLeadingNonLetters(rawCategory).takeIf { it.isNotBlank() } ?: return missing
    val matches = categories.filter { it.type == operation.type && matchesEntityName(it.name, rawCategory) }
    val resolved = resolveSingleMatch(matches, rawCategory, cleanCategory)
    return when {
        resolved != null -> OperationSuggestion.Ready(operation.type, operation.amount, resolved, description, date)
        matches.isEmpty() -> OperationSuggestion.NewCategory(
            operation.type, operation.amount, cleanCategory, iconFor(operation.suggestedIcon, rawCategory), description, date
        )
        else -> OperationSuggestion.AmbiguousCategory(operation.type, operation.amount, rawCategory, description, date)
    }
}

private fun learnedCategory(description: String?, type: CategoryType, categories: List<Category>, rules: List<CategoryRule>): Category? {
    val key = merchantKey(description) ?: return null
    val categoryId = rules.firstOrNull { it.merchantKey == key }?.categoryId ?: return null
    return categories.firstOrNull { it.id == categoryId && it.type == type }
}

private fun operationOf(command: BotCommand): OperationCommand? = when (command) {
    is BotCommand.AddExpense -> OperationCommand(
        CategoryType.EXPENSE, command.amount, command.category, command.description, command.suggestedCategoryIcon, command.operationDate
    )
    is BotCommand.AddIncome -> OperationCommand(
        CategoryType.INCOME, command.amount, command.category, command.description, command.suggestedCategoryIcon, command.operationDate
    )
    else -> null
}

private fun iconFor(suggestedIcon: String?, rawCategory: String): String =
    suggestedIcon?.trim()?.takeIf { it.isNotEmpty() } ?: extractLeadingNonLetters(rawCategory) ?: DEFAULT_CATEGORY_ICON

private fun resolveSingleMatch(matches: List<Category>, rawSearch: String, cleanSearch: String): Category? {
    if (matches.size <= 1) return matches.firstOrNull()
    matches.firstOrNull { it.name.equals(cleanSearch, ignoreCase = true) }?.let { return it }
    val leadingIcon = extractLeadingNonLetters(rawSearch) ?: return null
    return matches.firstOrNull { it.icon == leadingIcon }
}

fun originalOf(command: BotCommand): ForeignAmount? = when (command) {
    is BotCommand.AddExpense -> command.original
    is BotCommand.AddIncome -> command.original
    else -> null
}

fun repeatOf(command: BotCommand): Recurrence? {
    val (unit, interval) = when (command) {
        is BotCommand.AddExpense -> command.repeatUnit to command.repeatInterval
        is BotCommand.AddIncome -> command.repeatUnit to command.repeatInterval
        else -> return null
    }
    val recurrenceUnit = RecurrenceUnit.entries.firstOrNull { it.name.equals(unit?.trim(), ignoreCase = true) } ?: return null
    return runCatching { Recurrence(recurrenceUnit, interval?.toInt() ?: 1) }.getOrNull()
}
