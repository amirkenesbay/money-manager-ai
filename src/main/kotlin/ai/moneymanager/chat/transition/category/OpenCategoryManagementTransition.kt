package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openCategoryManagementTransition() {
    transition {
        name = "Open category management"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.CATEGORIES
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}