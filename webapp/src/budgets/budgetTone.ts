import type { Budget } from '../api/types'

export const BUDGET_NEAR_PERCENT = 80
export const BUDGET_OVER_PERCENT = 100

export type BudgetTone = 'ok' | 'near' | 'over'

export function budgetTone(percent: number): BudgetTone {
  if (percent >= BUDGET_OVER_PERCENT) return 'over'
  return percent >= BUDGET_NEAR_PERCENT ? 'near' : 'ok'
}

export const needsAttention = (budget: Budget): boolean => budgetTone(budget.percent) !== 'ok'

const TONE_COLORS: Record<BudgetTone, string> = {
  ok: 'var(--accent)',
  near: 'var(--warning)',
  over: 'var(--negative)',
}

export const budgetColor = (percent: number): string => TONE_COLORS[budgetTone(percent)]
