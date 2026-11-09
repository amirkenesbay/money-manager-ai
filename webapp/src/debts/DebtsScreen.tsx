import { useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import { serverMessage } from '../api/client'
import type { Currency, DebtItem, Debts, Group, Loan, LoanDirection, Me, PersonalDebt } from '../api/types'
import { formatDayLabel, formatMoney, formatSignedMoney, parseLocalDate } from '../format'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { PlusIcon } from '../ui/icons'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { SkeletonRows } from '../ui/Skeleton'
import { StateView } from '../ui/StateView'
import type { ToastMessage } from '../ui/Toast'
import type { Notify } from '../ui/useToast'
import { AmountSheet } from './AmountSheet'
import { burstConfetti, flyCoins } from '../ui/celebrate'
import { recordSettlement, remindDebtor, undoSettlement } from './debtsApi'
import { LoanDetail } from './LoanDetail'
import { MemberFace } from './MemberFace'
import { overdueDays } from './loanDates'
import { LoanForm } from './LoanForm'
import { repayLoan, undoRepayment } from './loansApi'
import { useDebts } from './useDebts'
import './debts.css'

const SKELETON_ROWS = 3
const EMPTY_ICON = '🤝'

type View = { kind: 'list' } | { kind: 'new'; person?: string; direction?: LoanDirection } | { kind: 'loan'; loanId: string }

interface Repayment {
  fromId: number
  toId: number
  amount: string
  origin: Element
}

const TOTAL_SELECTOR = '.debts-total-amount'

export function DebtsScreen({ me, group, notify, onBack }: { me: Me; group: Group; notify: Notify; onBack: () => void }) {
  const debts = useDebts(group.id)
  const [view, setView] = useState<View>({ kind: 'list' })
  const [repayment, setRepayment] = useState<Repayment | null>(null)
  const money = (amount: string) => formatMoney(amount, group.currency, me.language)
  const toList = () => setView({ kind: 'list' })

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

  const undoSettled = (settlementId: string) => void run(() => undoSettlement(group.id, settlementId), () => ({ title: strings.debtUndone }))

  const repay = (loan: Loan, amount: string) => void run(() => repayLoan(group.id, loan.id, amount).then((saved) => {
    if (!saved.open) burstConfetti()
    return saved
  }), (saved) => ({
    title: saved.open ? strings.loanRepaid : strings.loanClosed,
    detail: money(amount),
    celebrate: !saved.open,
    action: { label: strings.undo, onClick: () => void run(() => undoRepayment(group.id, loan.id, saved.repayments[0].id), () => ({ title: strings.debtUndone })) },
  }))

  if (view.kind === 'new') {
    return (
      <LoanForm
        group={group}
        language={me.language}
        initialPerson={view.person}
        initialDirection={view.direction}
        onBack={toList}
        onSaved={(loan) => {
          notify({ title: strings.loanSaved, detail: `${loan.person} · ${money(loan.amount)}` })
          debts.reload()
          toList()
        }}
      />
    )
  }

  const loan = view.kind === 'loan' && debts.state.status === 'ready' ? debts.state.data.loans.find((candidate) => candidate.id === view.loanId) : undefined
  if (loan) {
    return (
      <LoanDetail
        group={group}
        loan={loan}
        language={me.language}
        onBack={toList}
        onRepay={repay}
        onDeleted={() => {
          notify({ title: strings.loanDeleted })
          debts.reload()
          toList()
        }}
        onNewFor={(person, direction) => setView({ kind: 'new', person, direction })}
      />
    )
  }

  return (
    <>
      <ScreenHeader title={strings.debtsTitle} onBack={onBack} />
      {debts.state.status === 'loading' && <SkeletonRows count={SKELETON_ROWS} />}
      {debts.state.status === 'error' && <LoadError message={debts.state.message} onRetry={debts.reload} />}
      {debts.state.status === 'ready' && (
        <DebtsBody
          debts={debts.state.data}
          group={group}
          userId={me.userId}
          language={me.language}
          onOpenLoan={(loanId) => setView({ kind: 'loan', loanId })}
          onRemind={(debt) => void run(() => remindDebtor(group.id, debt.userId), () => ({ title: strings.debtReminded }))}
          onRepay={setRepayment}
          onUndo={undoSettled}
        />
      )}
      <BottomBar>
        <Button {...tourTarget('debts-add')} onClick={() => setView({ kind: 'new' })}><PlusIcon />{strings.loanAdd}</Button>
        <ScreenTour hint="DEBTS" />
      </BottomBar>
      {repayment && (
        <AmountSheet
          title={strings.debtAmountTitle}
          max={repayment.amount}
          currency={group.currency}
          language={me.language}
          onSave={(amount) => {
            setRepayment(null)
            void run(() => recordSettlement(group.id, repayment.fromId, repayment.toId, amount).then((settlement) => {
              const total = document.querySelector(TOTAL_SELECTOR)
              if (total && repayment.toId === me.userId) flyCoins(repayment.origin, total)
              else if (total) flyCoins(total, repayment.origin)
              return settlement
            }), (settlement) => ({
              title: strings.debtSaved,
              detail: money(amount),
              action: { label: strings.undo, onClick: () => undoSettled(settlement.id) },
            }))
          }}
          onClose={() => setRepayment(null)}
        />
      )}
    </>
  )
}

function DebtsBody({ debts, group, userId, language, onOpenLoan, onRemind, onRepay, onUndo }: {
  debts: Debts
  group: Group
  userId: number
  language: string
  onOpenLoan: (loanId: string) => void
  onRemind: (debt: PersonalDebt) => void
  onRepay: (repayment: Repayment) => void
  onUndo: (settlementId: string) => void
}) {
  const currency = group.currency
  const money = (amount: string) => formatMoney(amount, currency, language)
  const today = new Date()
  const loans = debts.loans.filter((loan) => loan.open).sort((left, right) => dueOrder(left) - dueOrder(right))
  const groupDebts = debts.mine.filter((debt) => Number(debt.amount) !== 0)
  const closed = debts.loans.filter((loan) => !loan.open)
  const empty = debts.loans.length === 0 && debts.mine.length === 0 && debts.others.length === 0
  const net = Number(debts.owedToMe) - Number(debts.iOwe)

  if (empty) return <StateView icon={EMPTY_ICON} title={strings.debtsNone} />
  return (
    <>
      <section className="debts-total" {...tourTarget('debts-tabs')}>
        <span className="debts-total-label">{net >= 0 ? strings.debtsOwedToMe : strings.debtsMine}</span>
        <span className={`debts-total-amount amount ${net >= 0 ? 'debt-owed' : 'debt-mine'}`}>{money(String(Math.abs(net)))}</span>
        <span className="caption">{strings.debtsSplit(money(debts.owedToMe), money(debts.iOwe))}</span>
      </section>
      {loans.length === 0 && groupDebts.length === 0
        ? <p className="caption centered">{strings.debtsNone}</p>
        : (
          <ul className="debt-people">
            {groupDebts.map((debt) => (
              <li key={debt.userId}>
                <PersonDebt debt={debt} groupName={group.name} userId={userId} currency={currency} language={language} onRemind={onRemind} onRepay={onRepay} />
              </li>
            ))}
            {loans.map((loan) => <li key={loan.id}><LoanRow loan={loan} currency={currency} language={language} onOpen={() => onOpenLoan(loan.id)} /></li>)}
          </ul>
        )}
      {closed.length > 0 && (
        <details className="debt-more">
          <summary className="link-button">{strings.loansClosed(closed.length)}</summary>
          <ul className="debt-people">
            {closed.map((loan) => <li key={loan.id}><LoanRow loan={loan} currency={currency} language={language} onOpen={() => onOpenLoan(loan.id)} /></li>)}
          </ul>
        </details>
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
                <span>{`${formatDayLabel(settlement.date, today, language)} · ${settlement.fromName} → ${settlement.toName}`}</span>
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

const dueOrder = (loan: Loan): number => (loan.dueDate ? parseLocalDate(loan.dueDate).getTime() : Number.MAX_SAFE_INTEGER)

const signedMoney = (owed: boolean, amount: string, currency: Currency, language: string): string =>
  formatSignedMoney(owed ? 'INCOME' : 'EXPENSE', amount, currency, language)

function LoanRow({ loan, currency, language, onOpen }: { loan: Loan; currency: Currency; language: string; onOpen: () => void }) {
  const today = new Date()
  const late = loan.open ? overdueDays(loan.dueDate, today) : 0
  const owed = loan.direction === 'LENT'
  return (
    <button type="button" className="debt-row pressable" onClick={onOpen}>
      <MemberFace name={loan.person} size="large" />
      <span className="debt-row-main">
        <span className="row-title">{loan.person}</span>
        {loan.open && loan.dueDate
          ? <span className={`due-chip ${late > 0 ? 'overdue' : ''}`}>{late > 0 ? strings.loanOverdue(late) : strings.loanDueOn(formatDayLabel(loan.dueDate, today, language))}</span>
          : <span className="caption">{loan.note ?? strings.loanLabel}</span>}
      </span>
      <span className={`debt-row-amount amount ${owed ? 'debt-owed' : 'debt-mine'}`}>{signedMoney(owed, loan.open ? loan.remaining : loan.amount, currency, language)}</span>
    </button>
  )
}

function PersonDebt({ debt, groupName, userId, currency, language, onRemind, onRepay }: {
  debt: PersonalDebt
  groupName: string
  userId: number
  currency: Currency
  language: string
  onRemind: (debt: PersonalDebt) => void
  onRepay: (repayment: Repayment) => void
}) {
  const [open, setOpen] = useState(false)
  const amount = Number(debt.amount)
  const owed = amount > 0
  const settle = (origin: Element) => onRepay({ fromId: owed ? debt.userId : userId, toId: owed ? userId : debt.userId, amount: String(Math.abs(amount)), origin })
  return (
    <>
      <button type="button" className="debt-row pressable" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
        <MemberFace name={debt.name} size="large" />
        <span className="debt-row-main">
          <span className="row-title">{debt.name}</span>
          <span className="caption">{groupName}</span>
        </span>
        <span className={`debt-row-amount amount ${owed ? 'debt-owed' : 'debt-mine'}`}>{signedMoney(owed, String(Math.abs(amount)), currency, language)}</span>
      </button>
      {open && (
        <div className="debt-details">
          <ul className="debt-items">
            {debt.items.map((item, index) => <DebtLine key={`${item.kind}-${index}`} item={item} currency={currency} language={language} />)}
          </ul>
          <div className="debt-actions">
            {owed && <Button size="small" variant="secondary" onClick={() => onRemind(debt)}>{strings.debtRemind}</Button>}
            <Button size="small" onClick={(event) => settle(event.currentTarget)}>{strings.debtSettle}</Button>
          </div>
        </div>
      )}
    </>
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
