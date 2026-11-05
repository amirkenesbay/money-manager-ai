import { useCallback, useState } from 'react'
import { serverMessage } from '../api/client'
import type { Group, Loan, LoanDirection } from '../api/types'
import { useAsync } from '../api/useAsync'
import { currencySymbol, formatDayLabel } from '../format'
import { parseAmount } from '../operation/amount'
import { CalendarSheet } from '../operation/CalendarSheet'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { CalendarIcon } from '../ui/icons'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { useShake } from '../ui/useShake'
import { type DueChoice, DUE_CHOICES, latestDueDate, quickDueDate } from './loanDates'
import { createLoan, fetchLoanPeople } from './loansApi'
import './debts.css'

const DIRECTIONS: LoanDirection[] = ['LENT', 'BORROWED']
const MAX_PERSON_LENGTH = 50

const directionLabel = (direction: LoanDirection) => (direction === 'LENT' ? strings.loanLend : strings.loanBorrow)

const dueLabel = (choice: DueChoice) => ({ none: strings.loanNoDue, week: strings.loanWeek, month: strings.loanMonth, pick: '' })[choice]

export function LoanForm({ group, language, initialPerson = '', initialDirection = 'LENT', onBack, onSaved }: {
  group: Group
  language: string
  initialPerson?: string
  initialDirection?: LoanDirection
  onBack: () => void
  onSaved: (loan: Loan) => void
}) {
  const today = new Date()
  const [direction, setDirection] = useState<LoanDirection>(initialDirection)
  const [amountText, setAmountText] = useState('')
  const [person, setPerson] = useState(initialPerson)
  const [due, setDue] = useState<DueChoice>('none')
  const [pickedDue, setPickedDue] = useState<string | null>(null)
  const [calendarOpen, setCalendarOpen] = useState(false)
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const loadPeople = useCallback((signal: AbortSignal) => fetchLoanPeople(group.id, signal), [group.id])
  const people = useAsync(loadPeople)
  const amount = parseAmount(amountText)
  const dueDate = due === 'pick' ? pickedDue : quickDueDate(due, today)
  const ready = amount !== null && person.trim() !== ''
  const [attempted, setAttempted] = useState(false)
  const amountShake = useShake()
  const personShake = useShake()
  useConfirmClosing(amountText.trim() !== '' || person.trim() !== '')

  const save = async () => {
    if (!ready || amount === null) {
      setAttempted(true)
      if (amount === null) amountShake.shake()
      else personShake.shake()
      return
    }
    setSaving(true)
    setError(null)
    try {
      const loan = await createLoan(group.id, { direction, person: person.trim(), amount, dueDate, note: note.trim() || null })
      notifyOutcome('success')
      onSaved(loan)
    } catch (failure) {
      notifyOutcome('error')
      setError(serverMessage(failure) ?? strings.saveFailed)
      setSaving(false)
    }
  }

  const suggestions = people.state.status === 'ready' ? people.state.data.filter((name) => name !== person) : []
  return (
    <>
      <ScreenHeader title={strings.loanNew} onBack={onBack} />
      <Segmented label={strings.loanNew} options={DIRECTIONS} value={direction} optionKey={(option) => option} optionLabel={directionLabel} onChange={setDirection} />
      <label className={`amount-field ${attempted && amount === null ? 'field-invalid' : ''} ${amountShake.shakeClass}`} onAnimationEnd={amountShake.onAnimationEnd}>
        <span className="field-label">{strings.amountLabel}</span>
        <span className="amount-input-row">
          <input className="amount-input amount" inputMode="decimal" autoComplete="off" placeholder="0" value={amountText} onChange={(event) => setAmountText(event.target.value)} />
          <span className="amount-currency" aria-hidden="true">{currencySymbol(group.currency)}</span>
        </span>
      </label>
      <fieldset className="field-group">
        <legend className="field-label">{direction === 'LENT' ? strings.loanTo : strings.loanFrom}</legend>
        <input
          className={`text-field ${attempted && person.trim() === '' ? 'field-invalid' : ''} ${personShake.shakeClass}`}
          onAnimationEnd={personShake.onAnimationEnd}
          aria-label={direction === 'LENT' ? strings.loanTo : strings.loanFrom}
          maxLength={MAX_PERSON_LENGTH}
          value={person}
          onChange={(event) => setPerson(event.target.value)}
        />
        {suggestions.length > 0 && (
          <div className="date-chips">
            {suggestions.map((name) => <button key={name} type="button" className="date-chip" onClick={() => setPerson(name)}>{name}</button>)}
          </div>
        )}
      </fieldset>
      <fieldset className="field-group">
        <legend className="field-label">{strings.loanDue}</legend>
        <div className="date-chips">
          {DUE_CHOICES.map((choice) => (
            choice === 'pick'
              ? <button key={choice} type="button" className="date-chip date-chip-calendar" aria-label={strings.pickDate} aria-pressed={due === 'pick'} onClick={() => setCalendarOpen(true)}><CalendarIcon /></button>
              : <button key={choice} type="button" className="date-chip" aria-pressed={due === choice} onClick={() => setDue(choice)}>{dueLabel(choice)}</button>
          ))}
        </div>
        {dueDate && <p className="caption">{strings.loanRemindOn(formatDayLabel(dueDate, today, language))}</p>}
      </fieldset>
      <input className="text-field" aria-label={strings.loanNotePlaceholder} placeholder={strings.loanNotePlaceholder} value={note} onChange={(event) => setNote(event.target.value)} />
      {error && <p className="error-text" role="alert">{error}</p>}
      <BottomBar>
        <Button disabled={saving} onClick={save}>{saving ? strings.saving : strings.save}</Button>
      </BottomBar>
      {calendarOpen && (
        <CalendarSheet
          value={pickedDue ?? quickDueDate('week', today) ?? ''}
          max={latestDueDate(today)}
          language={language}
          onPick={(picked) => {
            setPickedDue(picked)
            setDue('pick')
            setCalendarOpen(false)
          }}
          onClose={() => setCalendarOpen(false)}
        />
      )}
    </>
  )
}
