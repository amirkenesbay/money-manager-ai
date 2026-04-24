package ai.moneymanager.chat.transition.settings

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

private const val PERSONAL_GROUP_NAME_KEY = "group.default.personal_name"

private enum class LanguageChoice(
    val buttonType: MoneyManagerButtonType,
    val code: String
) {
    RU(MoneyManagerButtonType.LANGUAGE_RU, "ru"),
    EN(MoneyManagerButtonType.LANGUAGE_EN, "en"),
    KK(MoneyManagerButtonType.LANGUAGE_KK, "kk"),
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.languageDialogTransitions(
    userInfoService: UserInfoService,
    groupService: GroupService,
    localizationService: LocalizationService
) {
    LanguageChoice.entries.forEach { choice ->
        selectLanguageTransition(choice, userInfoService, groupService, localizationService)
    }
    routeToSettingsAfterLanguageTransition()
    routeToMenuAfterLanguageTransition()
    routeToOnboardingAfterLanguageTransition()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.selectLanguageTransition(
    choice: LanguageChoice,
    userInfoService: UserInfoService,
    groupService: GroupService,
    localizationService: LocalizationService
) {
    transition {
        name = "Select language: ${choice.code}"

        condition {
            from = MoneyManagerState.LANGUAGE_SELECT
            button = choice.buttonType
        }

        action {
            val updated = userInfoService.updateLanguage(user.id, choice.code)
            if (updated != null) {
                context.userInfo = updated
                context.languageJustChanged = true

                if (groupService.getUserGroups(user.id).isEmpty()) {
                    val personalName = localizationService.t(PERSONAL_GROUP_NAME_KEY, choice.code)
                    groupService.createPersonalGroup(user.id, personalName, choice.code)
                    val refreshed = userInfoService.getUserInfoByTelegramId(user.id)
                    if (refreshed != null) {
                        context.userInfo = refreshed
                    }
                }
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
