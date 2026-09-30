package ai.moneymanager.chat.transition.group

import ai.moneymanager.chat.state.MoneyManagerButtonType
import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openGroupManagementTransition() {
    transition {
        name = "Open group management"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.SHARED_ACCOUNTING
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }
}
