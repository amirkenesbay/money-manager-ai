package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.transition.ai.matchesEntityName
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
                groupId, creatorId, CategoryType.EXPENSE, command.amount, command.category, command.description, lang
            )
            is BotCommand.AddIncome -> prepareAdd(
                groupId, creatorId, CategoryType.INCOME, command.amount, command.category, command.description, lang
            )
            else -> AiPreparationResult.ImmediateResult(localizationService.t("ai.error.unknown_command", lang))
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        val lang = context.userInfo?.language
        if (action !is AiPendingAction.TransactionAction) return localizationService.t("ai.error.unknown_command", lang)
        return when (action) {
            is AiPendingAction.TransactionAction.Add -> executeAdd(action, lang)
        }
    }

    private fun prepareAdd(
        groupId: ObjectId,
        creatorId: Long,
        type: CategoryType,
        amount: Double,
        categoryName: String?,
        description: String?,
        lang: String?
    ): AiPreparationResult {
        if (amount <= 0) {
            return AiPreparationResult.ImmediateResult(localizationService.t("ai.transaction.amount_not_positive", lang))
        }
        if (categoryName.isNullOrBlank()) {
            return AiPreparationResult.ImmediateResult(categoryMissing(type, lang))
        }

        val available = categoryService.getCategoriesByGroupAndType(groupId, type)
        val matches = available.filter { matchesEntityName(it.name, categoryName) }
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(categoryName, available, lang))
            1 -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.TransactionAction.Add(
                    groupId = groupId,
                    creatorId = creatorId,
                    type = type,
                    amount = amount,
                    category = matches.first(),
                    description = description?.trim()?.takeIf { it.isNotEmpty() },
                    operationDate = LocalDate.now()
                )
            )
            else -> AiPreparationResult.ImmediateResult(
                localizationService.t("ai.transaction.category_ambiguous", lang, categoryName)
            )
        }
    }

    private fun executeAdd(action: AiPendingAction.TransactionAction.Add, lang: String?): String {
        val categoryId = action.category.id
            ?: return localizationService.t("ai.category.no_id", lang)
        financeOperationService.save(
            groupId = action.groupId,
            creatorId = action.creatorId,
            type = action.type,
            amount = action.amount,
            categoryId = categoryId,
            categoryName = action.category.name,
            categoryIcon = action.category.icon,
            operationDate = action.operationDate,
            description = action.description
        )
        val iconPart = action.category.icon?.let { "$it " } ?: ""
        val descPart = action.description?.let { " ($it)" } ?: ""
        val amountText = formatAmount(BigDecimal.valueOf(action.amount))
        val key = if (action.type == CategoryType.EXPENSE)
            "ai.transaction.add.success.expense"
        else
            "ai.transaction.add.success.income"
        return localizationService.t(key, lang, iconPart, action.category.name, amountText, descPart)
    }

    private fun categoryMissing(type: CategoryType, lang: String?): String {
        val key = if (type == CategoryType.EXPENSE)
            "ai.transaction.category_missing.expense"
        else
            "ai.transaction.category_missing.income"
        return localizationService.t(key, lang)
    }

    private fun categoryNotFound(name: String, available: List<Category>, lang: String?): String {
        if (available.isEmpty()) {
            return localizationService.t("ai.transaction.category_not_found_empty", lang, name)
        }
        val list = available.joinToString(", ") { it.name }
        return localizationService.t("ai.transaction.category_not_found", lang, name, list)
    }
}
