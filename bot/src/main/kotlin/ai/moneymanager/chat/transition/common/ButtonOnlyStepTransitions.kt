package ai.moneymanager.chat.transition.common

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

private val BUTTON_ONLY_STEPS = listOf(
    MoneyManagerState.LANGUAGE_SELECT,
    MoneyManagerState.CURRENCY_SELECT,
    MoneyManagerState.BALANCE_ONBOARDING_PROMPT,
    MoneyManagerState.GROUP_JOIN_CONFIRM,
)
private val TYPED_EVENTS = listOf(EventType.TEXT, EventType.VOICE, EventType.PHOTO, EventType.DOCUMENT)

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.repeatButtonOnlyStepTransitions() {
    BUTTON_ONLY_STEPS.forEach { step ->
        transition {
            name = "Repeat ${step.name.lowercase()} on a typed message"

            condition {
                from = step
                eventTypes = TYPED_EVENTS
            }

            then {
                to = step
            }
        }
    }
}
