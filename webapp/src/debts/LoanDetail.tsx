import { useState } from 'react'
import type { Group, Loan, LoanDirection } from '../api/types'
import { formatDayLabel, formatMoney } from '../format'
import { strings } from '../strings'
import { shareToTelegram } from '../telegram/share'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ConfirmSheet } from '../ui/ConfirmSheet'
import { ScreenHeader } from '../ui/ScreenHeader'
import { StateView } from '../ui/StateView'
import { AmountSheet } from './AmountSheet'
import { deleteLoan } from './loansApi'
import './debts.css'

const FULL_PERCENT = 100
const CLOSED_ICON = '🎉'

type SheetKind = 'repay' | 'delete' | null

export function LoanDetail({ group, loan, language, onBack, onRepay, onDeleted, onNewFor }: {
  group: Group
  loan: Loan
  language: string
  onBack: () => void
  onRepay: (loan: Loan, amount: string) => void
  onDeleted: (loan: Loan) => void
  onNewFor: (person: string, direction: LoanDirection) => void
}) {
  const [sheet, setSheet] = useState<SheetKind>(null)
  const today = new Date()
  const money = (amount: string) => formatMoney(amount, group.currency, language)
  const day = (date: string) => formatDayLabel(date, today, language)
  const lent = loan.direction === 'LENT'
  const events = [
    ...loan.repayments.map((repayment) => ({ key: repayment.id, label: `${day(repayment.date)} · ${strings.loanRepaidEvent}`, amount: repayment.amount })),
    { key: 'created', label: [day(loan.lentOn), lent ? strings.loanLentEvent : strings.loanBorrowedEvent, loan.note].filter(Boolean).join(' · '), amount: loan.amount },
  ]

  return (
    <>
      <ScreenHeader title={loan.person} onBack={onBack} />
      {loan.open
        ? (
          <section className="card debt-status">
            <span className="caption">{lent ? strings.loanOwesMe : strings.loanIOwe}</span>
            <span className={`amount ${lent ? 'debt-owed' : 'debt-mine'}`}>{money(loan.remaining)}</span>
            <span className="loan-progress" aria-hidden="true"><span style={{ width: `${(Number(loan.repaid) / Number(loan.amount)) * FULL_PERCENT}%` }} /></span>
            <span className="caption">{[strings.loanProgress(money(loan.repaid), money(loan.amount)), loan.dueDate && strings.loanDueOn(day(loan.dueDate))].filter(Boolean).join(' · ')}</span>
          </section>
        )
        : (
          <StateView icon={CLOSED_ICON} title={strings.loanClosed} body={money(loan.amount)}>
            <Button variant="secondary" onClick={() => onNewFor(loan.person, loan.direction)}>{strings.loanAgain(loan.person)}</Button>
          </StateView>
        )}
      <h2 className="section-title">{strings.debtsHistory}</h2>
      <ul className="debt-items">
        {events.map((event) => (
          <li key={event.key} className="debt-item"><span>{event.label}</span><span className="debt-item-amount">{money(event.amount)}</span></li>
        ))}
      </ul>
      <button type="button" className="link-button danger-link" onClick={() => setSheet('delete')}>{strings.loanDelete}</button>
      {loan.open && (
        <BottomBar>
          {lent && (
            <Button variant="secondary" onClick={() => shareToTelegram(strings.loanReminderText(money(loan.remaining), loan.dueDate && day(loan.dueDate)))}>
              {strings.loanRemind(loan.person)}
            </Button>
          )}
          <Button onClick={() => setSheet('repay')}>{strings.loanRepay}</Button>
        </BottomBar>
      )}
      {sheet === 'repay' && (
        <AmountSheet
          title={strings.loanRepayAmount}
          max={loan.remaining}
          currency={group.currency}
          language={language}
          onSave={(amount) => {
            setSheet(null)
            onRepay(loan, amount)
          }}
          onClose={() => setSheet(null)}
        />
      )}
      {sheet === 'delete' && (
        <ConfirmSheet
          title={strings.loanDeleteQuestion(loan.person)}
          confirmLabel={strings.loanDelete}
          onConfirm={() => deleteLoan(group.id, loan.id)}
          onDone={() => onDeleted(loan)}
          onClose={() => setSheet(null)}
        >
          <p className="hint negative">{strings.irreversible}</p>
        </ConfirmSheet>
      )}
    </>
  )
}
