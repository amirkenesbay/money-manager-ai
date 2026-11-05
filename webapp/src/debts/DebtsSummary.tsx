import type { Currency } from '../api/types'
import { formatMoney } from '../format'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { ChevronRightIcon } from '../ui/icons'
import { useDebts } from './useDebts'
import './debts.css'

export function DebtsSummary({ groupId, currency, language, onOpen }: { groupId: string; currency: Currency; language: string; onOpen: () => void }) {
  const debts = useDebts(groupId)
  if (debts.state.status !== 'ready') return null
  const balance = Number(debts.state.data.balance)
  if (balance === 0) {
    return (
      <button type="button" className="budget-calm pressable" onClick={onOpen}>
        <span className="budget-calm-dot" aria-hidden="true" />
        <span className="row-title">{strings.debtsNone}</span>
        <ChevronRightIcon />
      </button>
    )
  }
  const owed = balance > 0
  return (
    <section className="card debt-status" aria-label={strings.debtsTitle}>
      <span className="caption">{owed ? strings.debtsOwedToMe : strings.debtsMine}</span>
      <span className={`amount ${owed ? 'debt-owed' : 'debt-mine'}`}>{formatMoney(String(Math.abs(balance)), currency, language)}</span>
      <Button size="small" onClick={onOpen}>{strings.debtsTitle}</Button>
    </section>
  )
}
