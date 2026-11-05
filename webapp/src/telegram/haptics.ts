import { closingBehavior, hapticFeedback } from '@tma.js/sdk-react'

type Outcome = 'success' | 'error' | 'warning'

export function notifyOutcome(outcome: Outcome): void {
  try {
    hapticFeedback.notificationOccurred.ifAvailable(outcome)
  } catch {
    return
  }
}

export function tapImpact(): void {
  try {
    hapticFeedback.impactOccurred.ifAvailable('light')
  } catch {
    return
  }
}

export function holdImpact(): void {
  try {
    hapticFeedback.impactOccurred.ifAvailable('medium')
  } catch {
    return
  }
}

export function selectionTick(): void {
  try {
    hapticFeedback.selectionChanged.ifAvailable()
  } catch {
    return
  }
}

export function confirmClosing(enabled: boolean): void {
  try {
    if (enabled) closingBehavior.enableConfirmation.ifAvailable()
    else closingBehavior.disableConfirmation.ifAvailable()
  } catch {
    return
  }
}
