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
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class TransactionAiHandler(
    private val categoryService: CategoryService,
    private val financeOperationService: FinanceOperationService
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
        val groupId = context.userInfo?.activeGroupId
            ?: return AiPreparationResult.ImmediateResult(NO_ACTIVE_GROUP)
        val creatorId = context.userInfo?.telegramUserId
            ?: return AiPreparationResult.ImmediateResult(NO_USER)

        return when (command) {
            is BotCommand.AddExpense -> prepareAdd(
                groupId, creatorId, CategoryType.EXPENSE, command.amount, command.category, command.description
            )
            is BotCommand.AddIncome -> prepareAdd(
                groupId, creatorId, CategoryType.INCOME, command.amount, command.category, command.description
            )
            else -> AiPreparationResult.ImmediateResult(UNKNOWN_COMMAND_MESSAGE)
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        if (action !is AiPendingAction.TransactionAction) return UNKNOWN_COMMAND_MESSAGE
        return when (action) {
            is AiPendingAction.TransactionAction.Add -> executeAdd(action)
        }
    }

    private fun prepareAdd(
        groupId: ObjectId,
        creatorId: Long,
        type: CategoryType,
        amount: Double,
        categoryName: String?,
        description: String?
    ): AiPreparationResult {
        if (amount <= 0) {
            return AiPreparationResult.ImmediateResult(AMOUNT_NOT_POSITIVE)
        }
        if (categoryName.isNullOrBlank()) {
            return AiPreparationResult.ImmediateResult(categoryMissing(type))
        }

        val available = categoryService.getCategoriesByGroupAndType(groupId, type)
        val matches = available.filter { matchesEntityName(it.name, categoryName) }
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(categoryName, available))
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
            else -> AiPreparationResult.ImmediateResult(ambiguousCategory(categoryName))
        }
    }

    private fun executeAdd(action: AiPendingAction.TransactionAction.Add): String {
        val categoryId = action.category.id
            ?: return CATEGORY_HAS_NO_ID
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
        return "✅ ${typeLabel(action.type)} добавлен: $iconPart«${action.category.name}» — $amountText$descPart"
    }

    private fun typeLabel(type: CategoryType): String = when (type) {
        CategoryType.EXPENSE -> EXPENSE_LABEL
        CategoryType.INCOME -> INCOME_LABEL
    }

    private fun categoryMissing(type: CategoryType): String {
        val hint = when (type) {
            CategoryType.EXPENSE -> EXPENSE_HINT
            CategoryType.INCOME -> INCOME_HINT
        }
        return "❓ Укажи категорию. Например: $hint"
    }

    private fun ambiguousCategory(name: String): String =
        "❓ Нашёл несколько категорий «$name» — уточни название."

    private fun categoryNotFound(name: String, available: List<Category>): String {
        if (available.isEmpty()) {
            return "❌ В активной группе нет категорий для этого типа. Создай хотя бы одну: «создай категорию $name»."
        }
        val list = available.joinToString(", ") { it.name }
        return "❌ Категория «$name» не найдена. Доступные: $list."
    }

    companion object {
        private const val NO_ACTIVE_GROUP =
            "⚠️ Нет активной группы. Сначала создай группу — «создай группу Семья»."
        private const val NO_USER =
            "❌ Не удалось определить пользователя. Попробуй ещё раз."
        private const val UNKNOWN_COMMAND_MESSAGE =
            "❌ Не удалось обработать команду. Попробуй переформулировать."
        private const val AMOUNT_NOT_POSITIVE =
            "⚠️ Сумма должна быть больше нуля."
        private const val CATEGORY_HAS_NO_ID =
            "❌ Ошибка: у категории нет ID."
        private const val EXPENSE_LABEL = "Расход"
        private const val INCOME_LABEL = "Доход"
        private const val EXPENSE_HINT = "«кофе 500»"
        private const val INCOME_HINT = "«зарплата 500000»"
    }
}
