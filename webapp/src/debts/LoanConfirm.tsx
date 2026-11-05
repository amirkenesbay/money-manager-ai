import { useState } from 'react'
import { serverMessage } from '../api/client'
import type { Group, VoiceLoan } from '../api/types'
import { formatDayLabel, formatMoney } from '../format'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { StateView } from '../ui/StateView'
import { createLoan, repayLoan } from './loansApi'
import './debts.css'

const MISSING_ICON = '🤔'

export function LoanConfirm({ group, language, loan, onSayAgain, onClose, onSaved }: {
  group: Group
  language: string
  loan: VoiceLoan
  onSayAgain: () => void
  onClose: () => void
  onSaved: (title: string, detail: string) => void
}) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const money = (amount: string) => formatMoney(amount, group.currency, language)
  const repayment = loan.kind === 'REPAYMENT'
  const lent = loan.direction === 'LENT'
  const amount = loan.amount

  if (amount === null || (repayment && loan.loanId === null)) {
    return (
      <>
        <ScreenHeader title={strings.loanRepaymentTitle} onBack={onClose} />
        <StateView icon={MISSING_ICON} title={strings.loanMissing(loan.person)} tone="alert">
          <Button onClick={onSayAgain}>{strings.sayAgain}</Button>
        </StateView>
      </>
    )
  }

  const save = async () => {
    setSaving(true)
    setError(null)
    try {
      if (repayment && loan.loanId) {
        const saved = await repayLoan(group.id, loan.loanId, amount)
        onSaved(saved.open ? strings.loanRepaid : strings.loanClosed, `${saved.person} · ${money(amount)}`)
      } else {
        const saved = await createLoan(group.id, { direction: loan.direction, person: loan.person, amount, dueDate: loan.dueDate, note: loan.note })
        onSaved(strings.loanSaved, `${saved.person} · ${money(saved.amount)}`)
      }
      notifyOutcome('success')
    } catch (failure) {
      notifyOutcome('error')
      setError(serverMessage(failure) ?? strings.saveFailed)
      setSaving(false)
    }
  }

  const fields: [string, string][] = repayment
    ? [[lent ? strings.loanFrom : strings.loanTo, loan.person], ...(loan.remaining ? [[strings.loanIOwe, money(loan.remaining)] as [string, string]] : [])]
    : [
      [strings.categoryLabel, strings.loanKind(lent)],
      [lent ? strings.loanTo : strings.loanFrom, loan.person],
      [strings.loanDue, loan.dueDate ? formatDayLabel(loan.dueDate, new Date(), language) : strings.loanNoDue],
      ...(loan.note ? [[strings.commentLabel, loan.note] as [string, string]] : []),
    ]
  return (
    <>
      <ScreenHeader title={repayment ? strings.loanRepaymentTitle : strings.confirmOneTitle} onBack={onClose} />
      <section className="card draft-card">
        <span className={`amount draft-amount ${lent ? 'income' : ''}`}>{money(amount)}</span>
        <dl className="detail-fields draft-fields">
          {fields.map(([label, value]) => (
            <div key={label} className="detail-field"><dt className="caption">{label}</dt><dd>{value}</dd></div>
          ))}
        </dl>
      </section>
      {error && <p className="error-text" role="alert">{error}</p>}
      <BottomBar>
        <Button variant="secondary" onClick={onSayAgain}>{strings.sayAgain}</Button>
        <Button disabled={saving} onClick={save}>{saving ? strings.saving : strings.save}</Button>
      </BottomBar>
    </>
  )
}
