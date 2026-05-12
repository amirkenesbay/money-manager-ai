package ai.moneymanager.chat.transition.settings

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.transition.DialogBuilder

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.settingsDialogTransitions() {
    openSettingsTransition()
    openLanguagePickerFromSettingsTransition()
    backToMenuFromSettingsTransition()
    backToSettingsFromLanguagePickerTransition()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openSettingsTransition() {
    transition {
        name = "Open settings"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.SETTINGS
        }

        then {
            to = MoneyManagerState.SETTINGS
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openLanguagePickerFromSettingsTransition() {
    transition {
        name = "Open language picker from settings"

        condition {
            from = MoneyManagerState.SETTINGS
            button = MoneyManagerButtonType.OPEN_LANGUAGE_PICKER
        }

        action {
            context.languageReturnToSettings = true
        }

        then {
            to = MoneyManagerState.LANGUAGE_SELECT
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.backToMenuFromSettingsTransition() {
    transition {
        name = "Back to menu from settings"

        condition {
            from = MoneyManagerState.SETTINGS
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.backToSettingsFromLanguagePickerTransition() {
    transition {
        name = "Back to settings from language picker"

        condition {
            from = MoneyManagerState.LANGUAGE_SELECT
            button = MoneyManagerButtonType.BACK_TO_SETTINGS
        }

        action {
            context.languageReturnToSettings = false
        }

        then {
            to = MoneyManagerState.SETTINGS
        }
    }
}
