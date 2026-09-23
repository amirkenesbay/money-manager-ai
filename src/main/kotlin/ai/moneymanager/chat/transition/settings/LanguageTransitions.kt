package ai.moneymanager.chat.transition.settings

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.OnboardingService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

private enum class LanguageChoice(
    val buttonType: MoneyManagerButtonType,
    val code: String
) {
    RU(MoneyManagerButtonType.LANGUAGE_RU, "ru"),
    EN(MoneyManagerButtonType.LANGUAGE_EN, "en"),
    KK(MoneyManagerButtonType.LANGUAGE_KK, "kk"),
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.languageDialogTransitions(
    onboardingService: OnboardingService
) {
    LanguageChoice.entries.forEach { choice ->
        selectLanguageTransition(choice, onboardingService)
    }
    routeToSettingsAfterLanguageTransition()
    routeToMenuAfterLanguageTransition()
    routeToOnboardingAfterLanguageTransition()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.selectLanguageTransition(
    choice: LanguageChoice,
    onboardingService: OnboardingService
) {
    transition {
        name = "Select language: ${choice.code}"

        condition {
            from = MoneyManagerState.LANGUAGE_SELECT
            button = choice.buttonType
        }

        action {
            val updated = onboardingService.selectLanguage(user.id, choice.code)
            if (updated != null) {
                context.userInfo = updated
                context.languageJustChanged = true
            }
        }

        then {
            to = MoneyManagerState.LANGUAGE_SELECT
            noReply = true
            trigger { sameDialog = true }
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.routeToSettingsAfterLanguageTransition() {
    transition {
        name = "Route to settings after language choice"

        condition {
            from = MoneyManagerState.LANGUAGE_SELECT
            eventType = EventType.TRIGGERED
            guard { context.languageReturnToSettings }
        }

        action {
            context.languageReturnToSettings = false
        }

        then {
            to = MoneyManagerState.SETTINGS
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.routeToMenuAfterLanguageTransition() {
    transition {
        name = "Route to menu after language choice"

        condition {
            from = MoneyManagerState.LANGUAGE_SELECT
            eventType = EventType.TRIGGERED
            guard {
                !context.languageReturnToSettings &&
                    context.userInfo?.onboardingCompleted == true
            }
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.routeToOnboardingAfterLanguageTransition() {
    transition {
        name = "Route to balance onboarding after language choice"

        condition {
            from = MoneyManagerState.LANGUAGE_SELECT
            eventType = EventType.TRIGGERED
            guard {
                !context.languageReturnToSettings &&
                    context.userInfo?.onboardingCompleted != true
            }
        }

        then {
            to = MoneyManagerState.BALANCE_ONBOARDING_PROMPT
        }
    }
}
