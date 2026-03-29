package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.groupBackTransitions() {
    simpleTransition("Back to menu from group management",
        MoneyManagerState.GROUP_MANAGEMENT, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.MENU)

    simpleTransition("Back to group management from group list",
        MoneyManagerState.GROUP_LIST, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.GROUP_MANAGEMENT)

    transition {
        name = "Back to group actions from group invite"
        condition {
            from = MoneyManagerState.GROUP_INVITE_SHOW
            button = MoneyManagerButtonType.BACK_TO_MENU
            guard { context.inviteFromActions }
        }
        action { context.inviteFromActions = false }
        then { to = MoneyManagerState.GROUP_ACTIONS }
    }

    transition {
        name = "Back to group management from group invite (after creation)"
        condition {
            from = MoneyManagerState.GROUP_INVITE_SHOW
            button = MoneyManagerButtonType.BACK_TO_MENU
            guard { !context.inviteFromActions }
        }
        then { to = MoneyManagerState.GROUP_MANAGEMENT }
    }

    simpleTransition("Back to group actions from group members",
        MoneyManagerState.GROUP_MEMBERS, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.GROUP_ACTIONS)
}
