package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.transition.ai.matchesEntityName
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.CategoryService
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class CategoryAiHandler(
    private val categoryService: CategoryService
) : AiDomainHandler {

    override fun canHandle(command: BotCommand): Boolean = when (command) {
        is BotCommand.CreateCategory,
        is BotCommand.DeleteCategory,
        is BotCommand.RenameCategory,
        is BotCommand.ChangeCategoryIcon,
        is BotCommand.DeleteAllCategories,
        is BotCommand.ListCategories -> true
        else -> false
    }

    override fun canExecute(action: AiPendingAction): Boolean =
        action is AiPendingAction.CategoryAction

    override fun prepareAction(
        command: BotCommand,
        context: MoneyManagerContext
    ): AiPreparationResult {
        val groupId = context.userInfo?.activeGroupId
            ?: return AiPreparationResult.ImmediateResult(NO_ACTIVE_GROUP)

        return when (command) {
            is BotCommand.CreateCategory -> prepareCreate(command, groupId)
            is BotCommand.DeleteCategory -> prepareDelete(command, groupId)
            is BotCommand.RenameCategory -> prepareRename(command, groupId)
            is BotCommand.ChangeCategoryIcon -> prepareChangeIcon(command, groupId)
            is BotCommand.DeleteAllCategories -> prepareDeleteAll(groupId)
            is BotCommand.ListCategories -> prepareList(command, groupId, context)
            else -> AiPreparationResult.ImmediateResult(UNKNOWN_COMMAND_MESSAGE)
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        if (action !is AiPendingAction.CategoryAction) return UNKNOWN_COMMAND_MESSAGE
        return when (action) {
            is AiPendingAction.CategoryAction.Create -> executeCreate(action, context)
            is AiPendingAction.CategoryAction.Delete -> executeDelete(action)
            is AiPendingAction.CategoryAction.Rename -> executeRename(action)
            is AiPendingAction.CategoryAction.ChangeIcon -> executeChangeIcon(action)
            is AiPendingAction.CategoryAction.DeleteAll -> executeDeleteAll(action)
        }
    }

    // ========== PREPARE ==========

    private fun prepareCreate(cmd: BotCommand.CreateCategory, groupId: ObjectId): AiPreparationResult {
        val type = parseCategoryType(cmd.type)
            ?: return AiPreparationResult.ImmediateResult(
                "❓ Не понял тип категории «${cmd.name}». Уточни: это расход или доход?"
            )
        val icon = cmd.icon?.takeIf { isValidIcon(it) }
        return AiPreparationResult.RequiresConfirmation(
            AiPendingAction.CategoryAction.Create(cmd.name.trim(), type, icon)
        )
    }

    private fun prepareDelete(cmd: BotCommand.DeleteCategory, groupId: ObjectId): AiPreparationResult {
        val type = cmd.type?.let { parseCategoryType(it) }
        val matches = findCategories(groupId, cmd.name, type)
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(cmd.name))
            1 -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.CategoryAction.Delete(matches.first())
            )
            else -> AiPreparationResult.ImmediateResult(ambiguousType(cmd.name))
        }
    }

    private fun prepareRename(cmd: BotCommand.RenameCategory, groupId: ObjectId): AiPreparationResult {
        val type = cmd.type?.let { parseCategoryType(it) }
        val matches = findCategories(groupId, cmd.oldName, type)
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(cmd.oldName))
            1 -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.CategoryAction.Rename(matches.first(), cmd.newName.trim())
            )
            else -> AiPreparationResult.ImmediateResult(ambiguousType(cmd.oldName))
        }
    }

    private fun prepareChangeIcon(cmd: BotCommand.ChangeCategoryIcon, groupId: ObjectId): AiPreparationResult {
        if (!isValidIcon(cmd.newIcon)) {
            return AiPreparationResult.ImmediateResult(
                "❌ Иконка должна быть эмодзи (например 🎬, 💵, 🛒). Попробуй ещё раз."
            )
        }
        val type = cmd.type?.let { parseCategoryType(it) }
        val matches = findCategories(groupId, cmd.name, type)
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(cmd.name))
            1 -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.CategoryAction.ChangeIcon(matches.first(), cmd.newIcon.trim())
            )
            else -> AiPreparationResult.ImmediateResult(ambiguousType(cmd.name))
        }
    }

    private fun prepareDeleteAll(groupId: ObjectId): AiPreparationResult {
        val all = categoryService.getCategoriesByGroup(groupId)
        if (all.isEmpty()) {
            return AiPreparationResult.ImmediateResult("📋 У тебя пока нет категорий — удалять нечего.")
        }
        return AiPreparationResult.RequiresConfirmation(
            AiPendingAction.CategoryAction.DeleteAll(groupId, all.size)
        )
    }

    private fun prepareList(
        cmd: BotCommand.ListCategories,
        groupId: ObjectId,
        context: MoneyManagerContext
    ): AiPreparationResult {
        val type = cmd.type?.let { parseCategoryType(it) }
        if (type == null) {
            return AiPreparationResult.StateRedirect(MoneyManagerState.CATEGORY_LIST_SELECT_TYPE)
        }
        context.categoryTypeInput = type
        context.categories = categoryService.getCategoriesByGroupAndType(groupId, type)
        return AiPreparationResult.StateRedirect(MoneyManagerState.CATEGORY_LIST)
    }

    // ========== EXECUTE ==========

    private fun executeCreate(
        action: AiPendingAction.CategoryAction.Create,
        context: MoneyManagerContext
    ): String {
        val groupId = context.userInfo?.activeGroupId ?: return NO_ACTIVE_GROUP
        val created = categoryService.createCategory(action.name, action.icon, action.type, groupId)
            ?: return "❌ Категория «${action.name}» (${typeLabel(action.type)}) уже существует."
        val iconPart = created.icon?.let { "$it " } ?: ""
        return "✅ Категория $iconPart«${created.name}» (${typeLabel(created.type)}) создана."
    }

    private fun executeDelete(action: AiPendingAction.CategoryAction.Delete): String {
        val categoryId = action.category.id
            ?: return "❌ Ошибка: у категории нет ID."
        val success = categoryService.deleteCategory(categoryId)
        return if (success) {
            val iconPart = action.category.icon?.let { "$it " } ?: ""
            "✅ Категория $iconPart«${action.category.name}» удалена."
        } else {
            "❌ Не удалось удалить категорию «${action.category.name}»."
        }
    }

    private fun executeRename(action: AiPendingAction.CategoryAction.Rename): String {
        val categoryId = action.category.id
            ?: return "❌ Ошибка: у категории нет ID."
        val updated = categoryService.updateCategoryName(categoryId, action.newName)
            ?: return "❌ Не удалось переименовать категорию «${action.category.name}»."
        return "✅ Переименовано: «${action.category.name}» → «${updated.name}»."
    }

    private fun executeChangeIcon(action: AiPendingAction.CategoryAction.ChangeIcon): String {
        val categoryId = action.category.id
            ?: return "❌ Ошибка: у категории нет ID."
        val updated = categoryService.updateCategoryIcon(categoryId, action.newIcon)
            ?: return "❌ Не удалось изменить иконку «${action.category.name}»."
        return "✅ Иконка обновлена: ${updated.icon ?: "—"} «${updated.name}»."
    }

    private fun executeDeleteAll(action: AiPendingAction.CategoryAction.DeleteAll): String {
        val deletedCount = categoryService.deleteAllCategoriesForGroup(action.groupId)
        return if (deletedCount > 0) {
            "✅ Удалено категорий: $deletedCount."
        } else {
            "❌ Не удалось удалить категории."
        }
    }

    // ========== HELPERS ==========

    private fun findCategories(groupId: ObjectId, name: String, type: CategoryType?): List<Category> {
        val all = if (type != null) {
            categoryService.getCategoriesByGroupAndType(groupId, type)
        } else {
            categoryService.getCategoriesByGroup(groupId)
        }
        return all.filter { matchesEntityName(it.name, name) }
    }

    private fun isValidIcon(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 8) return false
        return trimmed.none { it.isLetterOrDigit() }
    }

    private fun parseCategoryType(raw: String?): CategoryType? {
        if (raw == null) return null
        return when (raw.trim().uppercase()) {
            "EXPENSE", "РАСХОД", "РАСХОДЫ" -> CategoryType.EXPENSE
            "INCOME", "ДОХОД", "ДОХОДЫ" -> CategoryType.INCOME
            else -> null
        }
    }

    private fun typeLabel(type: CategoryType): String = when (type) {
        CategoryType.EXPENSE -> "Расход"
        CategoryType.INCOME -> "Доход"
    }

    private fun categoryNotFound(name: String): String =
        "❌ Категория «$name» не найдена. Проверь название или посмотри список: «покажи категории»."

    private fun ambiguousType(name: String): String =
        "❓ Найдено несколько категорий «$name» (есть и расход, и доход). Уточни тип."

    companion object {
        private const val NO_ACTIVE_GROUP =
            "⚠️ Нет активной группы. Сначала создай группу — «создай группу Семья»."
        private const val UNKNOWN_COMMAND_MESSAGE =
            "❌ Не удалось обработать команду. Попробуй переформулировать."
    }
}
