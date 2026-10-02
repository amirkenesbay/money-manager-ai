import { useCallback, useState } from 'react'
import { serverMessage } from '../api/client'
import type { Balance, Currency, Group, MonthSummary, Operation } from '../api/types'
import { useAsync } from '../api/useAsync'
import { formatMoney, formatMonthTitle, formatShortDate, formatSignedMoney } from '../format'
import { strings } from '../strings'
import { LoadError } from '../ui/LoadError'
import { activateGroup, fetchGroups } from './dashboardApi'
import { useDashboard } from './useDashboard'

const TOP_CATEGORIES_LIMIT = 3
const FULL_PERCENT = 100

export function Dashboard({ initialGroupId, language }: { initialGroupId: string; language: string }) {
  const [groupId, setGroupId] = useState(initialGroupId)
  const [switchError, setSwitchError] = useState<string | null>(null)
  const groups = useAsync(fetchGroups)
  const dashboard = useDashboard(groupId)

  const switchGroup = useCallback(async (nextGroupId: string) => {
    setSwitchError(null)
    try {
      await activateGroup(nextGroupId)
      setGroupId(nextGroupId)
    } catch (error) {
      setSwitchError(serverMessage(error) ?? strings.groupSwitchFailed)
    }
  }, [])

  return (
    <main className="screen">
      {groups.state.status === 'ready' && (
        <GroupSwitcher groups={groups.state.data} activeGroupId={groupId} onSwitch={switchGroup} />
      )}
      {switchError && <p className="error-text" role="alert">{switchError}</p>}
      {dashboard.state.status === 'loading' && <p className="hint">{strings.loading}</p>}
      {dashboard.state.status === 'error' && <LoadError message={dashboard.state.message} onRetry={dashboard.reload} />}
      {dashboard.state.status === 'ready' && (
        <>
          <BalanceCard balance={dashboard.state.data.balance} language={language} />
          <MonthSummaryCard summary={dashboard.state.data.summary} language={language} />
          <RecentOperations
            operations={dashboard.state.data.operations.items}
            currency={dashboard.state.data.balance.currency}
            language={language}
          />
        </>
      )}
    </main>
  )
}

function GroupSwitcher({ groups, activeGroupId, onSwitch }: {
  groups: Group[]
  activeGroupId: string
  onSwitch: (groupId: string) => void
}) {
  const active = groups.find((group) => group.id === activeGroupId)
  if (groups.length < 2) return <h1>{active?.name ?? strings.dashboardTitle}</h1>
  return (
    <label className="group-switcher">
      <span className="hint">{strings.groupLabel}</span>
      <select value={activeGroupId} onChange={(event) => onSwitch(event.target.value)}>
        {groups.map((group) => (
          <option key={group.id} value={group.id}>{group.name}</option>
        ))}
      </select>
    </label>
  )
}

function BalanceCard({ balance, language }: { balance: Balance; language: string }) {
  const money = (amount: string) => formatMoney(amount, balance.currency, language)
  return (
    <section className="card">
      <p className="hint">{strings.balanceTitle}</p>
      <p className={`balance-total ${Number(balance.total) < 0 ? 'negative' : ''}`}>{money(balance.total)}</p>
      <dl className="breakdown">
        <dt>{strings.balanceInitial}</dt><dd>{money(balance.initial)}</dd>
        <IncomeExpenseRows income={balance.income} expense={balance.expense} currency={balance.currency} language={language} />
      </dl>
    </section>
  )
}

function MonthSummaryCard({ summary, language }: { summary: MonthSummary; language: string }) {
  const money = (amount: string) => formatMoney(amount, summary.currency, language)
  const topCategories = summary.expenseByCategory.slice(0, TOP_CATEGORIES_LIMIT)
  const maxTotal = Math.max(...topCategories.map((category) => Number(category.total)), 0)
  return (
    <section className="card">
      <h2>{strings.monthSummaryTitle(formatMonthTitle(summary.month, language))}</h2>
      {summary.operationCount === 0 ? (
        <p className="hint">{strings.monthEmpty}</p>
      ) : (
        <>
          <dl className="breakdown">
            <IncomeExpenseRows income={summary.income} expense={summary.expense} currency={summary.currency} language={language} />
            <dt>{strings.monthNet}</dt><dd className={Number(summary.net) < 0 ? 'negative' : 'positive'}>{money(summary.net)}</dd>
          </dl>
          {topCategories.length > 0 && (
            <ul className="categories" aria-label={strings.topExpensesTitle}>
              {topCategories.map((category) => (
                <li key={category.name}>
                  <span>{category.icon} {category.name}</span>
                  <span>{money(category.total)}</span>
                  <span className="bar" style={{ width: `${(Number(category.total) / maxTotal) * FULL_PERCENT}%` }} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}

function IncomeExpenseRows({ income, expense, currency, language }: {
  income: string
  expense: string
  currency: Currency
  language: string
}) {
  return (
    <>
      <dt>{strings.balanceIncome}</dt><dd className="positive">{formatSignedMoney('INCOME', income, currency, language)}</dd>
      <dt>{strings.balanceExpense}</dt><dd className="negative">{formatSignedMoney('EXPENSE', expense, currency, language)}</dd>
    </>
  )
}

function RecentOperations({ operations, currency, language }: {
  operations: Operation[]
  currency: Currency
  language: string
}) {
  return (
    <section className="card">
      <h2>{strings.recentOperationsTitle}</h2>
      {operations.length === 0 ? (
        <p className="hint">{strings.recentOperationsEmpty}</p>
      ) : (
        <ul className="operations">
          {operations.map((operation) => (
            <li key={operation.id}>
              <span className="operation-icon">{operation.categoryIcon}</span>
              <span className="operation-main">
                <span>{operation.categoryName}</span>
                <span className="hint">{[formatShortDate(operation.operationDate, language), operation.description].filter(Boolean).join(' · ')}</span>
              </span>
              <span className={operation.type === 'INCOME' ? 'positive' : 'negative'}>
                {formatSignedMoney(operation.type, operation.amount, currency, language)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
