package ai.moneymanager.chat.transition.balance

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.model.EventType
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val log = LoggerFactory.getLogger("BalanceTransitions")

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.balanceDialogTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService,
    financeOperationService: FinanceOperationService
) {
    onboardingPromptTransitions(groupService, userInfoService, financeOperationService)
    onboardingEnterAmountTransitions(groupService, userInfoService, financeOperationService)
    balanceViewTransitions(groupService, financeOperationService)
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.onboardingPromptTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService,
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Balance onboarding: start from zero"

        condition {
            from = MoneyManagerState.BALANCE_ONBOARDING_PROMPT
            button = MoneyManagerButtonType.BALANCE_START_FROM_ZERO
        }

        action {
            finishOnboarding(groupService, userInfoService, financeOperationService, BigDecimal.ZERO)
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    transition {
        name = "Balance onboarding: ask amount"

        condition {
            from = MoneyManagerState.BALANCE_ONBOARDING_PROMPT
            button = MoneyManagerButtonType.BALANCE_SET_AMOUNT
        }

        action {
            context.balanceAmountInputError = false
        }

        then {
            to = MoneyManagerState.BALANCE_ONBOARDING_ENTER_AMOUNT
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.onboardingEnterAmountTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService,
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Balance onboarding: save valid amount"

        condition {
            from = MoneyManagerState.BALANCE_ONBOARDING_ENTER_AMOUNT
            eventType = EventType.TEXT

            guard {
                parseAmount(update.message?.text)?.let { it >= BigDecimal.ZERO } == true
            }
        }

        action {
            val amount = parseAmount(update.message?.text) ?: BigDecimal.ZERO
            finishOnboarding(groupService, userInfoService, financeOperationService, amount)
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    transition {
        name = "Balance onboarding: invalid amount"

        condition {
            from = MoneyManagerState.BALANCE_ONBOARDING_ENTER_AMOUNT
            eventType = EventType.TEXT
        }

        action {
            context.balanceAmountInputError = true
        }

        then {
            to = MoneyManagerState.BALANCE_ONBOARDING_ENTER_AMOUNT
        }
    }

    transition {
        name = "Balance onboarding: cancel to zero from amount"

        condition {
            from = MoneyManagerState.BALANCE_ONBOARDING_ENTER_AMOUNT
            button = MoneyManagerButtonType.BALANCE_START_FROM_ZERO
        }

        action {
            finishOnboarding(groupService, userInfoService, financeOperationService, BigDecimal.ZERO)
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.balanceViewTransitions(
    groupService: GroupService,
    financeOperationService: FinanceOperationService
) {
    transition {
        name = "Open balance view from menu"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.BALANCE
        }

        action {
            loadCurrentBalance(groupService, financeOperationService)
        }

        then {
            to = MoneyManagerState.BALANCE_VIEW
        }
    }

    transition {
        name = "Back to menu from balance view"

        condition {
            from = MoneyManagerState.BALANCE_VIEW
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        action {
            loadCurrentBalance(groupService, financeOperationService)
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

internal fun ActionContext<MoneyManagerState, MoneyManagerContext>.loadCurrentBalance(
    groupService: GroupService,
    financeOperationService: FinanceOperationService
) {
    val userId = user.id
    val activeGroup = groupService.getActiveGroup(userId)
    if (activeGroup?.id == null) {
        context.currentBalance = null
        return
    }
    context.currentBalance = financeOperationService.calculateBalance(
        groupId = activeGroup.id,
        initialBalance = activeGroup.initialBalance
    )
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.finishOnboarding(
    groupService: GroupService,
    userInfoService: UserInfoService,
    financeOperationService: FinanceOperationService,
    amount: BigDecimal
) {
    val userId = user.id
    val activeGroup = groupService.getActiveGroup(userId)
    if (activeGroup?.id != null) {
        val updated = groupService.setInitialBalance(activeGroup.id, amount)
        log.info("Onboarding: set initialBalance={} for groupId={}", amount, activeGroup.id)
        context.currentGroup = updated
    } else {
        log.warn("Onboarding: active group not found for userId=$userId")
    }

    val updatedUser = userInfoService.markOnboardingCompleted(userId)
    if (updatedUser != null) {
        context.userInfo = updatedUser
    }

    loadCurrentBalance(groupService, financeOperationService)
    context.balanceAmountInputError = false
}

private fun parseAmount(raw: String?): BigDecimal? {
    val normalized = raw?.trim()
        ?.replace(" ", "")
        ?.replace(" ", "")
        ?.replace(",", ".")
        ?: return null
    if (normalized.isEmpty()) return null
    return runCatching { BigDecimal(normalized) }.getOrNull()
}
