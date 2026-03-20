package ai.moneymanager.chat.transition.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.groupBackTransitions() {
    transition {
        name = "Back to menu from group management"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    transition {
        name = "Back to group management from group list"

        condition {
            from = MoneyManagerState.GROUP_LIST
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }

    transition {
        name = "Back to group actions from group invite"

        condition {
            from = MoneyManagerState.GROUP_INVITE_SHOW
            button = MoneyManagerButtonType.BACK_TO_MENU

            guard {
                context.inviteFromActions
            }
        }

        action {
            context.inviteFromActions = false
        }

        then {
            to = MoneyManagerState.GROUP_ACTIONS
        }
    }

    transition {
        name = "Back to group management from group invite (after creation)"

        condition {
            from = MoneyManagerState.GROUP_INVITE_SHOW
            button = MoneyManagerButtonType.BACK_TO_MENU

            guard {
                !context.inviteFromActions
            }
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }

    transition {
        name = "Back to group actions from group members"

        condition {
            from = MoneyManagerState.GROUP_MEMBERS
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.GROUP_ACTIONS
        }
    }
}