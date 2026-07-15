package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.reply.common.buildInviteLink
import ai.moneymanager.chat.transition.ai.matchesEntityName
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import org.springframework.stereotype.Component

private const val OWNER_MARKER = " 👑"
private const val ACTIVE_MARKER = " ✅"
private const val LIST_LINE_SEPARATOR = "\n"

@Component
class GroupAiHandler(
    private val groupService: GroupService,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService
) : AiDomainHandler {

    override fun canHandle(command: BotCommand): Boolean = when (command) {
        is BotCommand.CreateGroup,
        is BotCommand.DeleteGroup,
        is BotCommand.ListGroups,
        is BotCommand.SwitchGroup -> true
        else -> false
    }

    override fun canExecute(action: AiPendingAction): Boolean =
        action is AiPendingAction.GroupAction

    override fun prepareAction(
        command: BotCommand,
        context: MoneyManagerContext
    ): AiPreparationResult {
        val lang = context.userInfo?.language
        val userId = context.userInfo?.telegramUserId
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_user", lang))

        return when (command) {
            is BotCommand.CreateGroup -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.GroupAction.Create(command.groupName.trim())
            )
            is BotCommand.DeleteGroup -> prepareDelete(command.groupName, userId, lang)
            is BotCommand.ListGroups -> AiPreparationResult.ImmediateResult(renderGroupList(userId, context, lang))
            is BotCommand.SwitchGroup -> switchGroup(command.groupName, userId, context, lang)
            else -> AiPreparationResult.ImmediateResult(localizationService.t("ai.error.unknown_command", lang))
        }
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String {
        val lang = context.userInfo?.language
        if (action !is AiPendingAction.GroupAction) return localizationService.t("ai.error.unknown_command", lang)
        val userId = context.userInfo?.telegramUserId
            ?: return localizationService.t("ai.error.no_user", lang)
        return when (action) {
            is AiPendingAction.GroupAction.Create -> executeCreate(action, userId, context, lang)
            is AiPendingAction.GroupAction.Delete -> executeDelete(action, userId, context, lang)
        }
    }

    private fun prepareDelete(name: String, userId: Long, lang: String?): AiPreparationResult {
        val matches = findGroups(userId, name)
        return when {
            matches.isEmpty() -> AiPreparationResult.ImmediateResult(
                localizationService.t("ai.group.not_found", lang, name)
            )
            matches.size > 1 -> AiPreparationResult.ImmediateResult(
                localizationService.t("ai.group.ambiguous", lang, name)
            )
            matches.first().ownerId != userId -> AiPreparationResult.ImmediateResult(
                localizationService.t("ai.group.delete.not_owner", lang)
            )
            else -> AiPreparationResult.RequiresConfirmation(
                AiPendingAction.GroupAction.Delete(matches.first())
            )
        }
    }

    private fun switchGroup(
        name: String,
        userId: Long,
        context: MoneyManagerContext,
        lang: String?
    ): AiPreparationResult {
        val matches = findGroups(userId, name)
        val message = when {
            matches.isEmpty() -> localizationService.t("ai.group.not_found", lang, name)
            matches.size > 1 -> localizationService.t("ai.group.ambiguous", lang, name)
            matches.first().id == context.userInfo?.activeGroupId ->
                localizationService.t("ai.group.switch.already", lang, matches.first().name)
            else -> {
                val group = matches.first()
                groupService.switchActiveGroup(userId, group.id!!)
                refreshUserInfo(userId, context)
                localizationService.t("ai.group.switched", lang, group.name)
            }
        }
        return AiPreparationResult.ImmediateResult(message)
    }

    private fun renderGroupList(userId: Long, context: MoneyManagerContext, lang: String?): String {
        val groups = groupService.getUserGroups(userId)
        if (groups.isEmpty()) return localizationService.t("ai.group.list.empty", lang)

        val activeGroupId = context.userInfo?.activeGroupId
        val lines = groups.joinToString(LIST_LINE_SEPARATOR) { group ->
            val markers = buildString {
                if (group.ownerId == userId) append(OWNER_MARKER)
                if (group.id == activeGroupId) append(ACTIVE_MARKER)
            }
            localizationService.t("ai.group.list.item", lang, group.name, group.memberIds.size, markers)
        }
        return buildString {
            append(localizationService.t("ai.group.list.title", lang))
            append(LIST_LINE_SEPARATOR).append(LIST_LINE_SEPARATOR)
            append(lines)
            append(LIST_LINE_SEPARATOR).append(LIST_LINE_SEPARATOR)
            append(localizationService.t("group.list.legend", lang))
        }
    }

    private fun executeCreate(
        action: AiPendingAction.GroupAction.Create,
        userId: Long,
        context: MoneyManagerContext,
        lang: String?
    ): String {
        val created = groupService.createGroup(userId, action.name)
            ?: return localizationService.t("ai.group.create.duplicate", lang, action.name)
        refreshUserInfo(userId, context)
        return localizationService.t("ai.group.created", lang, created.name, buildInviteLink(created.inviteToken))
    }

    private fun executeDelete(
        action: AiPendingAction.GroupAction.Delete,
        userId: Long,
        context: MoneyManagerContext,
        lang: String?
    ): String {
        val groupId = action.group.id
            ?: return localizationService.t("ai.group.delete.failed", lang, action.group.name)
        if (!groupService.deleteGroup(userId, groupId)) {
            return localizationService.t("ai.group.delete.failed", lang, action.group.name)
        }
        refreshUserInfo(userId, context)
        return localizationService.t("ai.group.deleted", lang, action.group.name)
    }

    private fun findGroups(userId: Long, name: String): List<MoneyGroup> =
        groupService.getUserGroups(userId).filter { matchesEntityName(it.name, name) }

    private fun refreshUserInfo(userId: Long, context: MoneyManagerContext) {
        userInfoService.getUserInfoByTelegramId(userId)?.let { context.userInfo = it }
    }
}
