package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.transition.common.textInputFlow
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.editGroupTransitions(
    groupService: GroupService,
    localizationService: LocalizationService
) {
    textInputFlow(
        flowName = "edit group name",
        sourceState = MoneyManagerState.GROUP_ACTIONS,
        editState = MoneyManagerState.GROUP_EDIT_ENTER_NAME,
        startButton = MoneyManagerButtonType.EDIT_GROUP,
        onSave = {
            val userId = user.id
            val groupId = context.currentGroup?.id ?: return@textInputFlow
            val newName = update.message.text ?: return@textInputFlow
            val lang = context.userInfo?.language

            val updatedGroup = groupService.updateGroupName(userId, groupId, newName)
            if (updatedGroup != null) {
                context.currentGroup = updatedGroup
                context.userGroups = groupService.getUserGroups(userId)
                context.renameConfirmation = localizationService.t("group.edit.success", lang, updatedGroup.name)
                context.textInputResponse = true
                context.groupNameDuplicateError = false
            } else {
                context.groupNameDuplicateError = true
                context.renameConfirmation = localizationService.t("group.edit.error.duplicate", lang, newName)
                context.textInputResponse = true
            }
        }
    )
}
