package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.reply.common.formatDescriptionSuffix
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.transition.ai.extractLeadingNonLetters
import ai.moneymanager.chat.transition.ai.matchesEntityName
import ai.moneymanager.chat.transition.ai.resolveOperationDate
import ai.moneymanager.chat.transition.ai.stripLeadingNonLetters
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.LocalizationService
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private const val SUCCESS_KEY_EXPENSE = "ai.transaction.add.success.expense"
private const val SUCCESS_KEY_INCOME = "ai.transaction.add.success.income"
private const val SUCCESS_NEW_CATEGORY_SUFFIX_KEY = "ai.transaction.add.success.new_category_suffix"
private const val NO_CATEGORY_ID_KEY = "ai.category.no_id"

@Component
class TransactionAiHandler(
    private val categoryService: CategoryService,
    private val financeOperationService: FinanceOperationService,
    private val localizationService: LocalizationService
) : AiDomainHandler {

    override fun canHandle(command: BotCommand): Boolean = when (command) {
        is BotCommand.AddExpense,
        is BotCommand.AddIncome -> true
        else -> false
    }

    override fun canExecute(action: AiPendingAction): Boolean =
        action is AiPendingAction.TransactionAction

    override fun prepareAction(
        command: BotCommand,
        context: MoneyManagerContext
    ): AiPreparationResult {
        val lang = context.userInfo?.language
        val groupId = context.userInfo?.activeGroupId
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_active_group", lang))
        val creatorId = context.userInfo?.telegramUserId
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_user", lang))

        return when (command) {
            is BotCommand.AddExpense -> prepareAdd(
                groupId, creatorId, CategoryType.EXPENSE, command.amount,
                command.category, command.description, command.suggestedCategoryIcon,
                resolveOperationDate(command.operationDate), context.aiCategoriesCache, lang
            )
            is BotCommand.AddIncome -> prepareAdd(
                groupId, creatorId, CategoryType.INCOME, command.amount,
                command.category, command.description, command.suggestedCategoryIcon,
                resolveOperationDate(command.operationDate), context.aiCategoriesCache, lang
            )
            else -> AiPreparationResult.ImmediateResult(localizationService.t("ai.error.unknown_command", lang))
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        val lang = context.userInfo?.language
        if (action !is AiPendingAction.TransactionAction) return localizationService.t("ai.error.unknown_command", lang)
        return when (action) {
            is AiPendingAction.TransactionAction.Add -> executeAdd(action, lang)
            is AiPendingAction.TransactionAction.AddWithNewCategory -> executeAddWithNewCategory(action, lang)
        }
    }

    private fun prepareAdd(
        groupId: ObjectId,
        creatorId: Long,
        type: CategoryType,
        amount: Double,
        categoryName: String?,
        description: String?,
        suggestedCategoryIcon: String?,
        operationDate: LocalDate,
        cachedCategories: List<Category>?,
        lang: String?
    ): AiPreparationResult {
        if (amount <= 0) {
            return AiPreparationResult.ImmediateResult(localizationService.t("ai.transaction.amount_not_positive", lang))
        }
        if (categoryName.isNullOrBlank()) {
            return AiPreparationResult.ImmediateResult(categoryMissing(type, lang))
        }

        val available = cachedCategories?.filter { it.type == type }
            ?: categoryService.getCategoriesByGroupAndType(groupId, type)
        val matches = available.filter { matchesEntityName(it.name, categoryName) }
        val trimmedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
        val cleanCategoryName = stripLeadingNonLetters(categoryName).takeIf { it.isNotBlank() }
            ?: return AiPreparationResult.ImmediateResult(categoryMissing(type, lang))
        val effectiveIcon = suggestedCategoryIcon
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: extractLeadingNonLetters(categoryName)
            ?: DEFAULT_CATEGORY_ICON
        val resolved = resolveSingleMatch(matches, categoryName, cleanCategoryName)
        return when {
            resolved != null -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.TransactionAction.Add(
                    groupId = groupId,
                    creatorId = creatorId,
                    type = type,
                    amount = amount,
                    category = resolved,
                    description = trimmedDescription,
                    operationDate = operationDate
                )
            )
            matches.isEmpty() -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.TransactionAction.AddWithNewCategory(
                    groupId = groupId,
                    creatorId = creatorId,
                    type = type,
                    amount = amount,
                    suggestedCategoryName = cleanCategoryName,
                    suggestedCategoryIcon = effectiveIcon,
                    description = trimmedDescription,
                    operationDate = operationDate
                )
            )
            else -> AiPreparationResult.ImmediateResult(
                localizationService.t("ai.transaction.category_ambiguous", lang, escapeHtml(categoryName))
            )
        }
    }

    private fun executeAdd(action: AiPendingAction.TransactionAction.Add, lang: String?): String =
        saveAndReport(
            groupId = action.groupId,
            creatorId = action.creatorId,
            type = action.type,
            amount = action.amount,
            category = action.category,
            operationDate = action.operationDate,
            description = action.description,
            isNewCategory = false,
            lang = lang
        )

    private fun executeAddWithNewCategory(
        action: AiPendingAction.TransactionAction.AddWithNewCategory,
        lang: String?
    ): String {
        val (category, isNew) = ensureCategory(action) ?: return localizationService.t(NO_CATEGORY_ID_KEY, lang)
        return saveAndReport(
            groupId = action.groupId,
            creatorId = action.creatorId,
            type = action.type,
            amount = action.amount,
            category = category,
            operationDate = action.operationDate,
            description = action.description,
            isNewCategory = isNew,
            lang = lang
        )
    }

    private fun saveAndReport(
        groupId: ObjectId,
        creatorId: Long,
        type: CategoryType,
        amount: Double,
        category: Category,
        operationDate: LocalDate,
        description: String?,
        isNewCategory: Boolean,
        lang: String?
    ): String {
        val categoryId = category.id ?: return localizationService.t(NO_CATEGORY_ID_KEY, lang)
        financeOperationService.save(
            groupId = groupId,
            creatorId = creatorId,
            type = type,
            amount = amount,
            categoryId = categoryId,
            categoryName = category.name,
            categoryIcon = category.icon,
            operationDate = operationDate,
            description = description
        )
        return successMessage(type, category, amount, description, lang, isNewCategory)
    }

    private fun ensureCategory(
        action: AiPendingAction.TransactionAction.AddWithNewCategory
    ): Pair<Category, Boolean>? {
        val created = categoryService.createCategory(
            name = action.suggestedCategoryName,
            icon = action.suggestedCategoryIcon,
            type = action.type,
            groupId = action.groupId
        )
        if (created != null) return created to true
        val existing = categoryService.getCategoriesByGroupAndType(action.groupId, action.type)
            .firstOrNull { matchesEntityName(it.name, action.suggestedCategoryName) }
            ?: return null
        return existing to false
    }

    private fun successMessage(
        type: CategoryType,
        category: Category,
        amount: Double,
        description: String?,
        lang: String?,
        isNewCategory: Boolean
    ): String {
        val key = if (type == CategoryType.EXPENSE) SUCCESS_KEY_EXPENSE else SUCCESS_KEY_INCOME
        val baseMessage = localizationService.t(
            key,
            lang,
            escapeHtml(formatIconPrefix(category.icon)),
            escapeHtml(category.name),
            formatAmount(BigDecimal.valueOf(amount)),
            formatDescriptionSuffix(description)
        )
        if (!isNewCategory) return baseMessage
        return baseMessage + localizationService.t(SUCCESS_NEW_CATEGORY_SUFFIX_KEY, lang)
    }

    private fun resolveSingleMatch(
        matches: List<Category>,
        rawSearch: String,
        cleanSearch: String
    ): Category? {
        if (matches.size <= 1) return matches.firstOrNull()
        matches.firstOrNull { it.name.equals(cleanSearch, ignoreCase = true) }
            ?.let { return it }
        val leadingIcon = extractLeadingNonLetters(rawSearch)
        if (leadingIcon != null) {
            matches.firstOrNull { it.icon == leadingIcon }
                ?.let { return it }
        }
        return null
    }

    private fun categoryMissing(type: CategoryType, lang: String?): String {
        val key = if (type == CategoryType.EXPENSE)
            "ai.transaction.category_missing.expense"
        else
            "ai.transaction.category_missing.income"
        return localizationService.t(key, lang)
    }
}
