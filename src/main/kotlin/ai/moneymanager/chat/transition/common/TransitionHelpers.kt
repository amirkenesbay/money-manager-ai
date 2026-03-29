package ai.moneymanager.chat.transition.common

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.Action
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.simpleTransition(
    name: String,
    from: MoneyManagerState,
    button: MoneyManagerButtonType,
    to: MoneyManagerState
) {
    transition {
        this.name = name
        condition {
            this.from = from
            this.button = button
        }
        then {
            this.to = to
        }
    }
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.simpleTransitionWithAction(
    name: String,
    from: MoneyManagerState,
    button: MoneyManagerButtonType,
    to: MoneyManagerState,
    action: Action<MoneyManagerState, MoneyManagerContext>
) {
    transition {
        this.name = name
        condition {
            this.from = from
            this.button = button
        }
        action(action)
        then {
            this.to = to
        }
    }
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.textInputFlow(
    flowName: String,
    sourceState: MoneyManagerState,
    editState: MoneyManagerState,
    startButton: MoneyManagerButtonType,
    cancelButton: MoneyManagerButtonType = MoneyManagerButtonType.CANCEL,
    onSave: Action<MoneyManagerState, MoneyManagerContext>
) {
    transition {
        name = "Start $flowName"
        condition {
            from = sourceState
            button = startButton
        }
        action {
            context.manualTextInputActive = true
        }
        then {
            to = editState
        }
    }

    transition {
        name = "Cancel $flowName"
        condition {
            from = editState
            button = cancelButton
        }
        action {
            context.manualTextInputActive = false
        }
        then {
            to = sourceState
        }
    }

    transition {
        name = "Save $flowName"
        condition {
            from = editState
            eventType = EventType.TEXT
        }
        action {
            context.manualTextInputActive = false
            onSave()
        }
        then {
            to = sourceState
        }
    }
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.confirmFlow(
    flowName: String,
    sourceState: MoneyManagerState,
    confirmState: MoneyManagerState,
    returnState: MoneyManagerState,
    triggerButton: MoneyManagerButtonType,
    cancelState: MoneyManagerState = sourceState,
    onStart: (Action<MoneyManagerState, MoneyManagerContext>)? = null,
    onCancel: (Action<MoneyManagerState, MoneyManagerContext>)? = null,
    onConfirm: Action<MoneyManagerState, MoneyManagerContext>
) {
    transition {
        name = "Start $flowName"
        condition {
            from = sourceState
            button = triggerButton
        }
        action {
            onStart?.invoke(this)
        }
        then {
            to = confirmState
        }
    }

    transition {
        name = "Confirm $flowName"
        condition {
            from = confirmState
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }
        action(onConfirm)
        then {
            to = returnState
        }
    }

    if (onCancel != null) {
        simpleTransitionWithAction("Cancel $flowName", confirmState, MoneyManagerButtonType.CANCEL, cancelState, onCancel)
    } else {
        simpleTransition("Cancel $flowName", confirmState, MoneyManagerButtonType.CANCEL, cancelState)
    }
}
