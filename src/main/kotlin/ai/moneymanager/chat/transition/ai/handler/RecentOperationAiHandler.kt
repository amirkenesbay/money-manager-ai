package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatIconPrefix
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.resolveCurrency
import ai.moneymanager.chat.transition.ai.matchesEntityName
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private const val LOOKUP_LIMIT = 20

@Component
class RecentOperationAiHandler(
    private val financeHistoryService: FinanceHistoryService,
    private val financeOperationService: FinanceOperationService,
    private val categoryService: CategoryService,
    private val groupService: GroupService,
    private val localizationService: LocalizationService
) : AiDomainHandler {

    override fun canHandle(command: BotCommand): Boolean = when (command) {
        is BotCommand.DeleteLastOperation,
        is BotCommand.EditLastOperation -> true
        else -> false
    }

    override fun canExecute(action: AiPendingAction): Boolean =
        action is AiPendingAction.RecentOperationAction

    override fun prepareAction(command: BotCommand, context: MoneyManagerContext): AiPreparationResult {
        val lang = context.userInfo?.language
        val groupId = context.userInfo?.activeGroupId
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_active_group", lang))

        return when (command) {
            is BotCommand.DeleteLastOperation -> prepareDelete(command.type, groupId, lang)
            is BotCommand.EditLastOperation -> prepareEdit(command, groupId, lang)
            else -> AiPreparationResult.ImmediateResult(localizationService.t("ai.error.unknown_command", lang))
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        val lang = context.userInfo?.language
        if (action !is AiPendingAction.RecentOperationAction) return localizationService.t("ai.error.unknown_command", lang)
        return when (action) {
            is AiPendingAction.RecentOperationAction.Delete -> executeDelete(action, lang)
            is AiPendingAction.RecentOperationAction.Edit -> executeEdit(action, lang)
        }
    }

    private fun prepareDelete(typeFilter: String?, groupId: ObjectId, lang: String?): AiPreparationResult {
        val operation = findLastOperation(groupId, typeFilter)
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.operation.not_found", lang))
        return AiPreparationResult.RequiresConfirmation(AiPendingAction.RecentOperationAction.Delete(operation))
    }

    private fun prepareEdit(command: BotCommand.EditLastOperation, groupId: ObjectId, lang: String?): AiPreparationResult {
        val operation = findLastOperation(groupId, command.type)
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.operation.not_found", lang))

        val newAmount = command.newAmount?.let { BigDecimal.valueOf(it) }
        if (newAmount != null && newAmount <= BigDecimal.ZERO) {
            return AiPreparationResult.ImmediateResult(localizationService.t("ai.transaction.amount_not_positive", lang))
        }

        val newCategory = command.newCategory?.let { name ->
            categoryService.getCategoriesByGroupAndType(groupId, operation.type)
                .firstOrNull { matchesEntityName(it.name, name) }
                ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.category.not_found", lang, escapeHtml(name)))
        }

        val newDate = command.newOperationDate?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
                ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.operation.edit.invalid_date", lang))
        }

        if (newAmount == null && newCategory == null && newDate == null) {
            return AiPreparationResult.ImmediateResult(localizationService.t("ai.operation.edit.nothing_to_change", lang))
        }

        return AiPreparationResult.RequiresConfirmation(
            AiPendingAction.RecentOperationAction.Edit(operation, newAmount, newCategory, newDate)
        )
    }

    private fun executeDelete(action: AiPendingAction.RecentOperationAction.Delete, lang: String?): String {
        val operation = action.operation
        val deleted = financeOperationService.deleteOperation(operation.groupId, operation.id!!)
        return if (deleted) {
            localizationService.t(
                "ai.operation.delete.success",
                lang,
                formatIconPrefix(operation.categoryIcon),
                escapeHtml(operation.categoryName),
                formatSignedAmount(operation.type, operation.amount, resolveCurrency(groupService, operation.groupId))
            )
        } else {
            localizationService.t("ai.operation.not_found", lang)
        }
    }

    private fun executeEdit(action: AiPendingAction.RecentOperationAction.Edit, lang: String?): String {
        val operation = action.operation
        val groupId = operation.groupId
        val operationId = operation.id!!

        action.newAmount?.let { financeOperationService.updateAmount(groupId, operationId, it) }
        action.newCategory?.let {
            financeOperationService.updateCategory(groupId, operationId, it.id!!, it.name, it.icon)
        }
        action.newOperationDate?.let { financeOperationService.updateDate(groupId, operationId, it) }

        val updated = financeOperationService.getOperation(groupId, operationId) ?: operation
        return localizationService.t(
            "ai.operation.edit.success",
            lang,
            formatIconPrefix(updated.categoryIcon),
            escapeHtml(updated.categoryName),
            formatSignedAmount(updated.type, updated.amount, resolveCurrency(groupService, groupId))
        )
    }

    private fun findLastOperation(groupId: ObjectId, typeFilter: String?): FinanceOperationEntity? {
        val type = typeFilter?.let { runCatching { CategoryType.valueOf(it.trim().uppercase()) }.getOrNull() }
        return financeHistoryService.getRecentOperations(groupId, LOOKUP_LIMIT)
            .firstOrNull { type == null || it.type == type }
    }
}
