import type { Category, Currency, Operation } from '../api/types'
import { formatMoney } from '../format'
import { strings } from '../strings'
import { useBudgets } from './useBudgets'

export function BudgetNote({ groupId, category, amount, editing, currency, language }: {
  groupId: string
  category: Category
  amount: string | null
  editing: Operation | null
  currency: Currency
  language: string
}) {
  const budgets = useBudgets(groupId)
  const budget = budgets.state.status === 'ready' ? budgets.state.data.find((candidate) => candidate.categoryId === category.id) : undefined
  if (!budget) return null
  const alreadyCounted = editing?.categoryId === category.id && editing.type === 'EXPENSE' ? Number(editing.amount) : 0
  const left = Number(budget.remaining) + alreadyCounted - Number(amount ?? 0)
  const name = `${category.icon} ${category.name}`
  const money = (value: number) => formatMoney(String(Math.abs(value)), currency, language)
  return left < 0
    ? <p className="budget-note over" role="status">{strings.budgetAfterOperationOver(name, money(left))}</p>
    : <p className="budget-note" role="status">{strings.budgetAfterOperation(name, money(left), formatMoney(budget.limit, currency, language))}</p>
}
