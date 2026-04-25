package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.transition.ai.matchesEntityName
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.LocalizationService
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class CategoryAiHandler(
    private val categoryService: CategoryService,
    private val localizationService: LocalizationService
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
        val lang = context.userInfo?.language
        val groupId = context.userInfo?.activeGroupId
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_active_group", lang))

        return when (command) {
            is BotCommand.CreateCategory -> prepareCreate(command, lang)
            is BotCommand.DeleteCategory -> prepareDelete(command, groupId, lang)
            is BotCommand.RenameCategory -> prepareRename(command, groupId, lang)
            is BotCommand.ChangeCategoryIcon -> prepareChangeIcon(command, groupId, lang)
            is BotCommand.DeleteAllCategories -> prepareDeleteAll(groupId, lang)
            is BotCommand.ListCategories -> prepareList(command, groupId, context)
            else -> AiPreparationResult.ImmediateResult(localizationService.t("ai.error.unknown_command", lang))
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        val lang = context.userInfo?.language
        if (action !is AiPendingAction.CategoryAction) return localizationService.t("ai.error.unknown_command", lang)
        return when (action) {
            is AiPendingAction.CategoryAction.Create -> executeCreate(action, context, lang)
            is AiPendingAction.CategoryAction.Delete -> executeDelete(action, lang)
            is AiPendingAction.CategoryAction.Rename -> executeRename(action, lang)
            is AiPendingAction.CategoryAction.ChangeIcon -> executeChangeIcon(action, lang)
            is AiPendingAction.CategoryAction.DeleteAll -> executeDeleteAll(action, lang)
        }
    }

    // ========== PREPARE ==========

    private fun prepareCreate(cmd: BotCommand.CreateCategory, lang: String?): AiPreparationResult {
        val type = parseCategoryType(cmd.type)
            ?: return AiPreparationResult.ImmediateResult(
                localizationService.t("ai.category.create.unknown_type", lang, cmd.name)
            )
        val icon = cmd.icon?.takeIf { isValidIcon(it) }
        return AiPreparationResult.RequiresConfirmation(
            AiPendingAction.CategoryAction.Create(cmd.name.trim(), type, icon)
        )
    }

    private fun prepareDelete(cmd: BotCommand.DeleteCategory, groupId: ObjectId, lang: String?): AiPreparationResult {
        val type = cmd.type?.let { parseCategoryType(it) }
        val matches = findCategories(groupId, cmd.name, type)
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(cmd.name, lang))
            1 -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.CategoryAction.Delete(matches.first())
            )
            else -> AiPreparationResult.ImmediateResult(ambiguousType(cmd.name, lang))
        }
    }

    private fun prepareRename(cmd: BotCommand.RenameCategory, groupId: ObjectId, lang: String?): AiPreparationResult {
        val type = cmd.type?.let { parseCategoryType(it) }
        val matches = findCategories(groupId, cmd.oldName, type)
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(cmd.oldName, lang))
            1 -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.CategoryAction.Rename(matches.first(), cmd.newName.trim())
            )
            else -> AiPreparationResult.ImmediateResult(ambiguousType(cmd.oldName, lang))
        }
    }

    private fun prepareChangeIcon(cmd: BotCommand.ChangeCategoryIcon, groupId: ObjectId, lang: String?): AiPreparationResult {
        if (!isValidIcon(cmd.newIcon)) {
            return AiPreparationResult.ImmediateResult(
                localizationService.t("ai.category.change_icon.invalid", lang)
            )
        }
        val type = cmd.type?.let { parseCategoryType(it) }
        val matches = findCategories(groupId, cmd.name, type)
        return when (matches.size) {
            0 -> AiPreparationResult.ImmediateResult(categoryNotFound(cmd.name, lang))
            1 -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.CategoryAction.ChangeIcon(matches.first(), cmd.newIcon.trim())
            )
            else -> AiPreparationResult.ImmediateResult(ambiguousType(cmd.name, lang))
        }
    }

    private fun prepareDeleteAll(groupId: ObjectId, lang: String?): AiPreparationResult {
        val all = categoryService.getCategoriesByGroup(groupId)
        if (all.isEmpty()) {
            return AiPreparationResult.ImmediateResult(localizationService.t("ai.category.delete_all.empty", lang))
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
        context: MoneyManagerContext,
        lang: String?
    ): String {
        val groupId = context.userInfo?.activeGroupId
            ?: return localizationService.t("ai.error.no_active_group", lang)
        val created = categoryService.createCategory(action.name, action.icon, action.type, groupId)
            ?: return localizationService.t(
                "ai.category.create.duplicate",
                lang,
                action.name,
                typeLabel(action.type, lang)
            )
        val iconPart = created.icon?.let { "$it " } ?: ""
        return localizationService.t(
            "ai.category.create.success",
            lang,
            iconPart,
            created.name,
            typeLabel(created.type, lang)
        )
    }

    private fun executeDelete(action: AiPendingAction.CategoryAction.Delete, lang: String?): String {
        val categoryId = action.category.id
            ?: return localizationService.t("ai.category.no_id", lang)
        val success = categoryService.deleteCategory(categoryId)
        return if (success) {
            val iconPart = action.category.icon?.let { "$it " } ?: ""
            localizationService.t("ai.category.delete.success", lang, iconPart, action.category.name)
        } else {
            localizationService.t("ai.category.delete.failed", lang, action.category.name)
        }
    }

    private fun executeRename(action: AiPendingAction.CategoryAction.Rename, lang: String?): String {
        val categoryId = action.category.id
            ?: return localizationService.t("ai.category.no_id", lang)
        val updated = categoryService.updateCategoryName(categoryId, action.newName)
            ?: return localizationService.t("ai.category.rename.failed", lang, action.category.name)
        return localizationService.t("ai.category.rename.success", lang, action.category.name, updated.name)
    }

    private fun executeChangeIcon(action: AiPendingAction.CategoryAction.ChangeIcon, lang: String?): String {
        val categoryId = action.category.id
            ?: return localizationService.t("ai.category.no_id", lang)
        val updated = categoryService.updateCategoryIcon(categoryId, action.newIcon)
            ?: return localizationService.t("ai.category.change_icon.failed", lang, action.category.name)
        return localizationService.t(
            "ai.category.change_icon.success",
            lang,
            updated.icon ?: "—",
            updated.name
        )
    }

    private fun executeDeleteAll(action: AiPendingAction.CategoryAction.DeleteAll, lang: String?): String {
        val deletedCount = categoryService.deleteAllCategoriesForGroup(action.groupId)
        return if (deletedCount > 0) {
            localizationService.t("ai.category.delete_all.success", lang, deletedCount)
        } else {
            localizationService.t("ai.category.delete_all.failed", lang)
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
        val normalized = raw.trim().uppercase()
        runCatching { CategoryType.valueOf(normalized) }.getOrNull()?.let { return it }
        LocalizationService.SUPPORTED_LANGUAGES.forEach { lang ->
            CategoryType.entries.forEach { type ->
                val label = localizationService.t("ai.type.${type.name.lowercase()}", lang).uppercase()
                if (normalized == label) return type
            }
        }
        return null
    }

    private fun typeLabel(type: CategoryType, lang: String?): String = when (type) {
        CategoryType.EXPENSE -> localizationService.t("ai.type.expense", lang)
        CategoryType.INCOME -> localizationService.t("ai.type.income", lang)
    }

    private fun categoryNotFound(name: String, lang: String?): String =
        localizationService.t("ai.category.not_found", lang, name)

    private fun ambiguousType(name: String, lang: String?): String =
        localizationService.t("ai.category.ambiguous", lang, name)
}
