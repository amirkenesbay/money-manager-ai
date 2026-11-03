import type { Currency } from '../api/types'
import { strings } from '../strings'
import { ChevronRightIcon } from '../ui/icons'
import { BudgetRow } from './BudgetRow'
import { needsAttention } from './budgetTone'
import { useBudgets } from './useBudgets'

export function BudgetsSummary({ groupId, currency, language, onOpenAll }: {
  groupId: string
  currency: Currency
  language: string
  onOpenAll: () => void
}) {
  const budgets = useBudgets(groupId)
  if (budgets.state.status !== 'ready' || budgets.state.data.length === 0) return null
  const all = budgets.state.data
  const attention = all.filter(needsAttention)
  if (attention.length === 0) {
    return (
      <button type="button" className="budget-calm pressable" onClick={onOpenAll}>
        <span className="budget-calm-dot" aria-hidden="true" />
        <span className="row-title">{strings.budgetsCalm}</span>
        <span className="caption">{all.length}</span>
        <ChevronRightIcon />
      </button>
    )
  }
  const fine = all.length - attention.length
  return (
    <section className="budgets" aria-label={strings.budgetsTitle}>
      <span className="recent-head">
        <h2 className="section-title">{strings.budgetsTitle}</h2>
        <button type="button" className="link-button" onClick={onOpenAll}>{strings.budgetsAll(all.length)}</button>
      </span>
      <div className="budget-card">
        {attention.map((budget) => <BudgetRow key={budget.categoryId} budget={budget} currency={currency} language={language} />)}
        {fine > 0 && <p className="caption centered">{strings.budgetsMoreFine(fine)}</p>}
      </div>
    </section>
  )
}
