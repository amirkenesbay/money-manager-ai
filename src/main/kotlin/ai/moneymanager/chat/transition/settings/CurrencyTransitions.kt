package ai.moneymanager.chat.transition.settings

import ai.moneymanager.chat.transition.common.handleGroupCreated
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

private enum class CurrencyChoice(
    val buttonType: MoneyManagerButtonType,
    val currency: Currency
) {
    KZT(MoneyManagerButtonType.CURRENCY_KZT, Currency.KZT),
    USD(MoneyManagerButtonType.CURRENCY_USD, Currency.USD),
    RUB(MoneyManagerButtonType.CURRENCY_RUB, Currency.RUB),
    EUR(MoneyManagerButtonType.CURRENCY_EUR, Currency.EUR),
    KGS(MoneyManagerButtonType.CURRENCY_KGS, Currency.KGS),
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.currencyDialogTransitions(
    groupService: GroupService
) {
    CurrencyChoice.entries.forEach { choice ->
        selectCurrencyTransition(choice, groupService)
    }
    routeToSettingsAfterCurrencyTransition()
    routeToOnboardingAfterCurrencyTransition()
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.selectCurrencyTransition(
    choice: CurrencyChoice,
    groupService: GroupService
) {
    transition {
        name = "Select currency: ${choice.currency}"

        condition {
            from = MoneyManagerState.CURRENCY_SELECT
            button = choice.buttonType
        }

        action {
            if (context.currencyForPendingGroupCreation) {
                val name = context.groupNameInput.orEmpty()
                context.handleGroupCreated(groupService.createGroup(user.id, name, choice.currency))
            } else {
                val activeGroup = groupService.getActiveGroup(user.id)
                if (activeGroup?.id != null) {
                    val updated = groupService.updateCurrency(user.id, activeGroup.id, choice.currency)
                    if (updated != null) {
                        context.currentGroup = updated
                        context.currencyJustChanged = true
                    }
                }
            }
        }

        then {
            to = MoneyManagerState.CURRENCY_SELECT
            noReply = true
            trigger { sameDialog = true }
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.routeToSettingsAfterCurrencyTransition() {
    transition {
        name = "Route to settings after currency choice"

        condition {
            from = MoneyManagerState.CURRENCY_SELECT
            eventType = EventType.TRIGGERED
            guard { context.currencyReturnToSettings }
        }

        action {
            context.currencyReturnToSettings = false
        }

        then {
            to = MoneyManagerState.SETTINGS
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.routeToOnboardingAfterCurrencyTransition() {
    transition {
        name = "Route to balance onboarding after currency choice"

        condition {
            from = MoneyManagerState.CURRENCY_SELECT
            eventType = EventType.TRIGGERED
            guard { !context.currencyReturnToSettings && !context.currencyForPendingGroupCreation }
        }

        then {
            to = MoneyManagerState.BALANCE_ONBOARDING_PROMPT
        }
    }
}
