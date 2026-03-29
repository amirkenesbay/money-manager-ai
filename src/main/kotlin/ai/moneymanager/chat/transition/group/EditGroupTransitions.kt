package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.transition.common.textInputFlow
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.editGroupTransitions(
    groupService: GroupService
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

            val updatedGroup = groupService.updateGroupName(userId, groupId, newName)
            if (updatedGroup != null) {
                context.currentGroup = updatedGroup
                context.userGroups = groupService.getUserGroups(userId)
                context.renameConfirmation = "✅ Название изменено на «${updatedGroup.name}»"
                context.textInputResponse = true
                context.groupNameDuplicateError = false
            } else {
                context.groupNameDuplicateError = true
                context.renameConfirmation = "❌ Группа с названием «$newName» уже существует"
                context.textInputResponse = true
            }
        }
    )
}
