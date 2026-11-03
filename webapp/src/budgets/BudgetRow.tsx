import type { Budget, Currency } from '../api/types'
import { formatMoney } from '../format'
import { strings } from '../strings'
import { Meter } from '../ui/Meter'
import { budgetColor, budgetTone } from './budgetTone'
import './budgets.css'

export function BudgetRow({ budget, currency, language }: { budget: Budget; currency: Currency; language: string }) {
  const tone = budgetTone(budget.percent)
  const money = (amount: string) => formatMoney(amount, currency, language)
  const status = tone === 'over'
    ? strings.budgetOver(money(String(-Number(budget.remaining))))
    : strings.budgetLeft(money(budget.remaining))
  return (
    <div className={`budget-row ${tone}`}>
      <span className="budget-line">
        <span className="row-title">{budget.icon} {budget.name}</span>
        <span className="budget-status caption">{status}</span>
      </span>
      <Meter percent={budget.percent} color={budgetColor(budget.percent)} />
      <span className="budget-line caption">
        <span>{strings.budgetSpentOf(money(budget.spent), money(budget.limit))}</span>
        <span>{budget.percent}%</span>
      </span>
    </div>
  )
}
