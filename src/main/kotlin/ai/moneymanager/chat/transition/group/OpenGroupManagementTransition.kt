package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
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