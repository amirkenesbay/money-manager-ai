package ai.moneymanager.chat.transition.category

import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryBackTransitions() {
    simpleTransition("Back to menu from category management",
        MoneyManagerState.CATEGORY_MANAGEMENT, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.MENU)

    simpleTransition("Back to category management from select type",
        MoneyManagerState.CATEGORY_LIST_SELECT_TYPE, MoneyManagerButtonType.CANCEL, MoneyManagerState.CATEGORY_MANAGEMENT)

    simpleTransition("Back to select type from category list",
        MoneyManagerState.CATEGORY_LIST, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.CATEGORY_LIST_SELECT_TYPE)
}
