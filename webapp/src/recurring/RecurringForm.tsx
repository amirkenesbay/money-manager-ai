import { useState } from 'react'
import type { CategoryType, Group, RecurrenceUnit, RecurringMode, RecurringPayment } from '../api/types'
import { useCategories } from '../categories/useCategories'
import { CATEGORY_TYPES } from '../categories/categoryTypes'
import { toIsoDate } from '../format'
import { CategoryTiles } from '../operation/CategoryTiles'
import { parseAmount } from '../operation/amount'
import { MAX_COMMENT_LENGTH } from '../operation/limits'
import { strings } from '../strings'
import { AmountInput } from '../ui/AmountInput'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ChipSelect } from '../ui/ChipSelect'
import { ConfirmSheet } from '../ui/ConfirmSheet'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { SkeletonRows } from '../ui/Skeleton'
import { useSubmit } from '../ui/useSubmit'
import { intervalUnitOf, RECURRENCE_UNITS } from './recurrence'
import { createRecurring, deleteRecurring, type RecurringInput, updateRecurring } from './recurringApi'
import '../reminders/reminders.css'

const MODES: RecurringMode[] = ['AUTO', 'REMIND']
const CATEGORY_SKELETON_ROWS = 3
const MIN_INTERVAL = 1
const MAX_INTERVAL = 365

export function RecurringForm({ group, payment, onBack, onSaved, onDeleted, onLimit }: {
  group: Group
  payment: RecurringPayment | null
  onBack: () => void
  onSaved: (payment: RecurringPayment, created: boolean) => void
  onDeleted: (payment: RecurringPayment) => void
  onLimit: () => void
}) {
  const categories = useCategories(group.id)
  const [type, setType] = useState<CategoryType>(payment?.type ?? 'EXPENSE')
  const [amountText, setAmountText] = useState(payment?.amount ?? '')
  const [categoryId, setCategoryId] = useState<string | null>(payment?.categoryId ?? null)
  const [description, setDescription] = useState(payment?.description ?? '')
  const [unit, setUnit] = useState<RecurrenceUnit>(payment?.unit ?? 'MONTH')
  const [intervalText, setIntervalText] = useState(String(payment?.interval ?? MIN_INTERVAL))
  const [startDate, setStartDate] = useState(payment?.startDate ?? toIsoDate(new Date()))
  const [endDate, setEndDate] = useState(payment?.endDate ?? '')
  const [mode, setMode] = useState<RecurringMode>(payment?.mode ?? 'AUTO')
  const [deleteOpen, setDeleteOpen] = useState(false)
  const submit = useSubmit()

  const amount = parseAmount(amountText)
  const amountInvalid = amountText.trim() !== '' && amount === null
  const interval = Number(intervalText)
  const intervalValid = Number.isInteger(interval) && interval >= MIN_INTERVAL && interval <= MAX_INTERVAL
  const typeCategories = categories.state.status === 'ready' ? categories.state.data.filter((category) => category.type === type) : []
  const selected = typeCategories.find((category) => category.id === categoryId) ?? typeCategories[0] ?? null
  const ready = amount !== null && intervalValid && selected !== null && startDate !== ''

  const save = async () => {
    if (!ready) return
    const input: RecurringInput = {
      type, amount, categoryId: selected.id, description: description.trim() || null,
      unit, interval, startDate, endDate: endDate || null, mode,
    }
    const result = await submit.run(() => (payment ? updateRecurring(group.id, payment.id, input) : createRecurring(group.id, input)), strings.saveFailed)
    if (result.ok) onSaved(result.value, payment === null)
    else if (result.error.code === 'RECURRING_LIMIT_REACHED') onLimit()
  }

  return (
    <>
      <ScreenHeader title={payment ? strings.editRecurringTitle : strings.newRecurringTitle} onBack={onBack} />
      <Segmented label={strings.operationTypeLabel} options={CATEGORY_TYPES} value={type} optionKey={(option) => option} optionLabel={strings.operationType} onChange={(next) => { setType(next); setCategoryId(null) }} />
      <AmountInput label={strings.amountLabel} value={amountText} currency={group.currency} invalid={amountInvalid} onChange={setAmountText} />
      <fieldset className="field-group">
        <legend className="field-label">{strings.categoryLabel}</legend>
        {categories.state.status === 'loading' && <SkeletonRows count={CATEGORY_SKELETON_ROWS} />}
        {categories.state.status === 'error' && <LoadError message={categories.state.message} onRetry={categories.reload} />}
        {categories.state.status === 'ready' && <CategoryTiles categories={typeCategories} selectedId={selected?.id ?? null} onSelect={setCategoryId} />}
      </fieldset>
      <input className="text-field" aria-label={strings.commentLabel} placeholder={strings.commentPlaceholder} maxLength={MAX_COMMENT_LENGTH} value={description} onChange={(event) => setDescription(event.target.value)} />
      <fieldset className="field-group">
        <legend className="field-label">{strings.repeatLabel}</legend>
        <div className="interval-row">
          <span className="caption">{strings.intervalLabel}</span>
          <input className="text-field interval-input" inputMode="numeric" aria-label={strings.intervalLabel} aria-invalid={!intervalValid} value={intervalText} onChange={(event) => setIntervalText(event.target.value)} />
          <ChipSelect
            label={strings.repeatLabel}
            value={unit}
            options={RECURRENCE_UNITS.map((option) => ({ value: option, label: strings.intervalUnit(intervalUnitOf(option)) }))}
            onChange={(value) => setUnit(value as RecurrenceUnit)}
          />
        </div>
      </fieldset>
      <label className="field-group">
        <span className="field-label">{strings.recurringStartLabel}</span>
        <input className="text-field" type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
      </label>
      <label className="field-group">
        <span className="field-label">{strings.recurringEndLabel}</span>
        <input className="text-field" type="date" min={startDate} value={endDate} onChange={(event) => setEndDate(event.target.value)} />
      </label>
      <fieldset className="field-group">
        <legend className="field-label">{strings.recurringModeLabel}</legend>
        <Segmented label={strings.recurringModeLabel} options={MODES} value={mode} optionKey={(option) => option} optionLabel={(option) => strings.recurringModes[option]} onChange={setMode} />
      </fieldset>
      {payment && <button type="button" className="link-button danger-link" onClick={() => setDeleteOpen(true)}>{strings.deleteRecurring}</button>}
      {submit.error && submit.error.code !== 'RECURRING_LIMIT_REACHED' && <p className="error-text" role="alert">{submit.error.message}</p>}
      <BottomBar>
        <Button disabled={submit.pending || !ready} onClick={save}>{submit.pending ? strings.saving : payment ? strings.saveChanges : strings.save}</Button>
      </BottomBar>
      {deleteOpen && payment && (
        <ConfirmSheet
          title={strings.deleteRecurringQuestion(payment.description ?? payment.categoryName)}
          confirmLabel={strings.delete}
          onConfirm={() => deleteRecurring(group.id, payment.id)}
          onDone={() => onDeleted(payment)}
          onClose={() => setDeleteOpen(false)}
        >
          <p className="hint">{strings.deleteRecurringWarning}</p>
        </ConfirmSheet>
      )}
    </>
  )
}
