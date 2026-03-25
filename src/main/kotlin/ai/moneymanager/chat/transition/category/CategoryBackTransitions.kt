package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryBackTransitions() {
    transition {
        name = "Back to menu from category management"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    transition {
        name = "Back to category management from select type"

        condition {
            from = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }

    transition {
        name = "Back to select type from category list"

        condition {
            from = MoneyManagerState.CATEGORY_LIST
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
        }
    }
}