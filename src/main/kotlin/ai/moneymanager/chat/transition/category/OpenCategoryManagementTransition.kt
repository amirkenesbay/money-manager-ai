package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
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