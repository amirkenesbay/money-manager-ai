package ai.moneymanager.chat.transition.category

import ai.moneymanager.chat.state.MoneyManagerButtonType
import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openCategoryManagementTransition(
    groupService: GroupService
) {
    transition {
        name = "Open category management"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.CATEGORIES
        }

        action {
            val activeGroupId = context.userInfo?.activeGroupId
            context.activeGroupName = if (activeGroupId != null) {
                groupService.getGroup(activeGroupId)?.name
            } else null
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}
