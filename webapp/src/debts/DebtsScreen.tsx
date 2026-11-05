import { useState } from 'react'
import { serverMessage } from '../api/client'
import type { Currency, DebtItem, Debts, Group, Me, PersonalDebt } from '../api/types'
import { currencySymbol, formatDayLabel, formatMoney, formatSignedMoney } from '../format'
import { parseAmount } from '../operation/amount'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { Button } from '../ui/Button'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Sheet } from '../ui/Sheet'
import { SkeletonRows } from '../ui/Skeleton'
import { StateView } from '../ui/StateView'
import type { ToastMessage } from '../ui/Toast'
import type { Notify } from '../ui/useToast'
import { recordSettlement, remindDebtor, undoSettlement } from './debtsApi'
import { useDebts } from './useDebts'
import './debts.css'

const SKELETON_ROWS = 3
const EMPTY_ICON = '🤝'

interface Repayment {
  fromId: number
  toId: number
  amount: string
}

export function DebtsScreen({ me, group, notify, onBack }: { me: Me; group: Group; notify: Notify; onBack: () => void }) {
  const debts = useDebts(group.id)
  const [repayment, setRepayment] = useState<Repayment | null>(null)
  const money = (amount: string) => formatMoney(amount, group.currency, me.language)

  const run = async <T,>(action: () => Promise<T>, toastOf: (result: T) => Omit<ToastMessage, 'id'>) => {
    try {
      const result = await action()
      notifyOutcome('success')
      notify(toastOf(result))
      debts.reload()
    } catch (error) {
      notifyOutcome('error')
      notify({ title: serverMessage(error) ?? strings.saveFailed })
    }
  }

  const undo = (settlementId: string) => void run(() => undoSettlement(group.id, settlementId), () => ({ title: strings.debtUndone }))

  return (
    <>
      <ScreenHeader title={strings.debtsTitle} onBack={onBack} />
      {debts.state.status === 'loading' && <SkeletonRows count={SKELETON_ROWS} />}
      {debts.state.status === 'error' && <LoadError message={debts.state.message} onRetry={debts.reload} />}
      {debts.state.status === 'ready' && (
        <DebtsBody
          debts={debts.state.data}
          userId={me.userId}
          currency={group.currency}
          language={me.language}
          onRemind={(debt) => void run(() => remindDebtor(group.id, debt.userId), () => ({ title: strings.debtReminded }))}
          onRepay={setRepayment}
          onUndo={undo}
        />
      )}
      {repayment && (
        <RepaymentSheet
          repayment={repayment}
          currency={group.currency}
          onSave={(amount) => {
            setRepayment(null)
            void run(() => recordSettlement(group.id, repayment.fromId, repayment.toId, amount), (settlement) => ({
              title: strings.debtSaved,
              detail: money(amount),
              action: { label: strings.undo, onClick: () => undo(settlement.id) },
            }))
          }}
          onClose={() => setRepayment(null)}
        />
      )}
    </>
  )
}

function DebtsBody({ debts, userId, currency, language, onRemind, onRepay, onUndo }: {
  debts: Debts
  userId: number
  currency: Currency
  language: string
  onRemind: (debt: PersonalDebt) => void
  onRepay: (repayment: Repayment) => void
  onUndo: (settlementId: string) => void
}) {
  const money = (amount: string) => formatMoney(amount, currency, language)
  const settled = debts.mine.length === 0 && debts.others.length === 0
  return (
    <>
      {settled
        ? <StateView icon={EMPTY_ICON} title={strings.debtsNone} />
        : debts.mine.length > 0 && (
          <section className="card">
            {debts.mine.map((debt) => (
              <PersonDebt key={debt.userId} debt={debt} userId={userId} currency={currency} language={language} onRemind={onRemind} onRepay={onRepay} />
            ))}
          </section>
        )}
      {debts.others.length > 0 && (
        <details className="debt-more">
          <summary className="link-button">{strings.debtsOthers}</summary>
          <ul className="debt-items">
            {debts.others.map((transfer) => (
              <li key={`${transfer.fromId}-${transfer.toId}`} className="debt-item">
                <span>{`${transfer.fromName} → ${transfer.toName}`}</span>
                <span className="debt-item-amount">{money(transfer.amount)}</span>
              </li>
            ))}
          </ul>
        </details>
      )}
      {debts.history.length > 0 && (
        <details className="debt-more">
          <summary className="link-button">{strings.debtsHistory}</summary>
          <ul className="debt-items">
            {debts.history.map((settlement) => (
              <li key={settlement.id} className="debt-item">
                <span>{`${formatDayLabel(settlement.date, new Date(), language)} · ${settlement.fromName} → ${settlement.toName}`}</span>
                <span className="debt-item-amount">{money(settlement.amount)}</span>
                {settlement.canUndo && <button type="button" className="link-button" onClick={() => onUndo(settlement.id)}>{strings.undo}</button>}
              </li>
            ))}
          </ul>
        </details>
      )}
    </>
  )
}

function PersonDebt({ debt, userId, currency, language, onRemind, onRepay }: {
  debt: PersonalDebt
  userId: number
  currency: Currency
  language: string
  onRemind: (debt: PersonalDebt) => void
  onRepay: (repayment: Repayment) => void
}) {
  const [open, setOpen] = useState(false)
  const amount = Number(debt.amount)
  const owed = amount > 0
  const total = formatMoney(String(Math.abs(amount)), currency, language)
  const repay = (fromId: number, toId: number) => onRepay({ fromId, toId, amount: String(Math.abs(amount)) })
  return (
    <div>
      <div className="debt-person">
        <button type="button" className="debt-person-main" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
          <span className="row-title">{debt.name}</span>
          <span className={`caption ${owed ? 'debt-owed' : 'debt-mine'}`}>{owed ? strings.debtOwesMe(total) : strings.debtIOwe(total)}</span>
        </button>
        {owed
          ? (
            <>
              <Button size="small" variant="secondary" onClick={() => onRemind(debt)}>{strings.debtRemind}</Button>
              <Button size="small" variant="secondary" onClick={() => repay(debt.userId, userId)}>{strings.debtReceived}</Button>
            </>
          )
          : <Button size="small" variant="secondary" onClick={() => repay(userId, debt.userId)}>{strings.debtPaid}</Button>}
      </div>
      {open && (
        <ul className="debt-items">
          {debt.items.map((item, index) => <DebtLine key={`${item.kind}-${index}`} item={item} currency={currency} language={language} />)}
        </ul>
      )}
    </div>
  )
}

function DebtLine({ item, currency, language }: { item: DebtItem; currency: Currency; language: string }) {
  const label = {
    EXPENSE: [item.icon, item.title].filter(Boolean).join(' '),
    SETTLEMENT: strings.debtRepayment,
    OFFSET: strings.debtOffset,
    EARLIER: strings.debtEarlier,
  }[item.kind]
  const date = item.date ? `, ${formatDayLabel(item.date, new Date(), language)}` : ''
  const amount = Number(item.amount)
  return (
    <li className="debt-item">
      <span>{label + date}</span>
      <span className="debt-item-amount">{formatSignedMoney(amount >= 0 ? 'INCOME' : 'EXPENSE', String(Math.abs(amount)), currency, language)}</span>
    </li>
  )
}

function RepaymentSheet({ repayment, currency, onSave, onClose }: {
  repayment: Repayment
  currency: Currency
  onSave: (amount: string) => void
  onClose: () => void
}) {
  const [text, setText] = useState(repayment.amount)
  const amount = parseAmount(text)
  const tooMuch = amount !== null && Number(amount) > Number(repayment.amount)
  return (
    <Sheet label={strings.debtAmountTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.debtAmountTitle}</h2>
      <label className="amount-field">
        <span className="amount-input-row">
          <input className="amount-input amount" inputMode="decimal" autoComplete="off" aria-label={strings.debtAmountTitle} value={text} onChange={(event) => setText(event.target.value)} />
          <span className="amount-currency" aria-hidden="true">{currencySymbol(currency)}</span>
        </span>
      </label>
      <Button disabled={amount === null || tooMuch} onClick={() => amount && onSave(amount)}>{strings.save}</Button>
      <Button variant="secondary" onClick={onClose}>{strings.cancel}</Button>
    </Sheet>
  )
}
