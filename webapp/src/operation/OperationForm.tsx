import { useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import { serverMessage } from '../api/client'
import type { Category, CategoryType, Group, Operation, RecurrenceUnit } from '../api/types'
import { useCategories } from '../categories/useCategories'
import { currencySymbol, formatDayLabel, formatMoney, toIsoDate } from '../format'
import { createOperation, type OperationInput, replaceOperation } from '../operations/operationsApi'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { CalendarIcon } from '../ui/icons'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { useShake } from '../ui/useShake'
import { HoldButton } from '../ui/HoldButton'
import { SkeletonRows } from '../ui/Skeleton'
import { groupAmountInput, HUGE_AMOUNT, parseAmount } from './amount'
import { CalendarSheet } from './CalendarSheet'
import { DeleteSheet } from './DeleteSheet'
import './operation.css'
import { MAX_COMMENT_LENGTH } from './limits'
import { CategoryTiles } from './CategoryTiles'
import { repeatChoiceLabel, REPEAT_CHOICES } from '../recurring/recurrence'
import { startRepeating } from '../recurring/repeatOperation'
import { TagsField } from './TagsField'
import { tagsOf, withoutTags, withTags } from '../operations/tags'
import { BudgetNote } from '../budgets/BudgetNote'
import { ExpenseSplit } from '../debts/ExpenseSplit'
import { type Share, shareOf } from '../debts/share'

const OPERATION_TYPES: CategoryType[] = ['EXPENSE', 'INCOME']
const QUICK_DAYS_AGO = [0, 1, 2]
const CATEGORY_SKELETON_ROWS = 2

export type SaveResult = { operation: Operation; created: boolean; note?: string }

export function OperationForm({ group, userId, language, operation, initialType = 'EXPENSE', initialCategoryId, onBack, onSaved, onDeleted }: {
  group: Group
  userId: number
  language: string
  operation: Operation | null
  initialType?: CategoryType
  initialCategoryId?: string
  onBack: () => void
  onSaved: (result: SaveResult) => void
  onDeleted: (operation: Operation) => void
}) {
  const categories = useCategories(group.id)

  return (
    <>
      <ScreenHeader title={operation ? strings.editOperationTitle : strings.newOperationTitle} onBack={onBack} />
      {categories.state.status === 'loading' && <SkeletonRows count={CATEGORY_SKELETON_ROWS} />}
      {categories.state.status === 'error' && <LoadError message={categories.state.message} onRetry={categories.reload} />}
      {categories.state.status === 'ready' && (
        <FormFields group={group} userId={userId} language={language} operation={operation} initialType={initialType} initialCategoryId={initialCategoryId} categories={categories.state.data} onCategoryCreated={categories.add} onSaved={onSaved} onDeleted={onDeleted} />
      )}
    </>
  )
}

function FormFields({ group, userId, language, operation, initialType, initialCategoryId, categories, onCategoryCreated, onSaved, onDeleted }: {
  group: Group
  userId: number
  language: string
  operation: Operation | null
  initialType: CategoryType
  initialCategoryId?: string
  categories: Category[]
  onCategoryCreated: (category: Category) => void
  onSaved: (result: SaveResult) => void
  onDeleted: (operation: Operation) => void
}) {
  const today = toIsoDate(new Date())
  const [type, setType] = useState<CategoryType>(operation?.type ?? initialType)
  const [amountText, setAmountText] = useState(operation ? operation.amount : '')
  const [categoryId, setCategoryId] = useState<string | null>(operation?.categoryId ?? initialCategoryId ?? null)
  const [date, setDate] = useState(operation?.operationDate ?? today)
  const [comment, setComment] = useState(withoutTags(operation?.description ?? null))
  const [tags, setTags] = useState(tagsOf(operation?.description ?? null))
  const [repeat, setRepeat] = useState<RecurrenceUnit | null>(null)
  const [share, setShare] = useState<Share | null>(() => (operation ? shareOf(operation) : null))
  const [calendarOpen, setCalendarOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [attempted, setAttempted] = useState(false)
  const amountShake = useShake()

  const typeCategories = categories.filter((category) => category.type === type)
  const selectedCategory = typeCategories.find((category) => category.id === categoryId) ?? typeCategories[0] ?? null
  const amount = parseAmount(amountText)
  const amountInvalid = (attempted || amountText.trim() !== '') && amount === null
  const huge = amount !== null && Number(amount) >= HUGE_AMOUNT
  const quickDates = QUICK_DAYS_AGO.map((daysAgo) => toIsoDate(daysBefore(daysAgo)))
  const customDate = !quickDates.includes(date)
  const splitting = group.splitExpenses && type === 'EXPENSE'
  const dirty = operation === null ? amountText.trim() !== '' || comment.trim() !== '' || tags.length > 0 : true

  useConfirmClosing(dirty)

  const selectType = (next: CategoryType) => {
    setType(next)
    setCategoryId(null)
  }

  const save = async () => {
    if (amount === null) {
      setAttempted(true)
      amountShake.shake()
      return
    }
    if (selectedCategory === null) return
    const split = splitting && share ? { payerId: share.payerId, participantIds: share.participantIds } : {}
    const input: OperationInput = { type, amount, categoryId: selectedCategory.id, operationDate: date, description: withTags(comment, tags), ...split }
    setSaving(true)
    setSaveError(null)
    try {
      const saved = operation ? await replaceOperation(group.id, operation.id, input) : await createOperation(group.id, input)
      notifyOutcome('success')
      const note = repeat === null ? undefined : await startRepeating(group.id, saved, { unit: repeat, interval: 1 })
      onSaved({ operation: saved, created: operation === null, note })
    } catch (error) {
      notifyOutcome('error')
      setSaveError(serverMessage(error) ?? strings.saveFailed)
      setSaving(false)
    }
  }

  return (
    <>
      <div {...tourTarget('form-type')}>
        <Segmented label={strings.operationTypeLabel} options={OPERATION_TYPES} value={type} optionKey={(option) => option} optionLabel={strings.operationType} onChange={selectType} />
      </div>
      {!operation && <ScreenTour hint="OPERATION_FORM" />}
      <label className={`amount-field ${amountInvalid ? 'field-invalid' : ''} ${huge ? 'field-warning' : ''} ${amountShake.shakeClass}`} onAnimationEnd={amountShake.onAnimationEnd}>
        <span className="field-label">{strings.amountLabel}</span>
        <span className="amount-input-row">
          <input
            className={`amount-input amount ${type === 'INCOME' ? 'income' : ''}`}
            inputMode="decimal"
            autoComplete="off"
            placeholder="0"
            aria-invalid={amountInvalid}
            value={amountText}
            onChange={(event) => setAmountText(groupAmountInput(event.target.value))}
          />
          <span className="amount-currency" aria-hidden="true">{currencySymbol(group.currency)}</span>
        </span>
      </label>
      {amountInvalid && <p className="field-message negative" role="alert">{strings.amountInvalid}</p>}
      {huge && amount !== null && <p className="field-message warning" role="status">{strings.amountHuge(formatMoney(amount, group.currency, language))}</p>}

      <fieldset className="field-group" {...tourTarget('form-categories')}>
        <legend className="field-label">{strings.categoryLabel}</legend>
        <CategoryTiles categories={typeCategories} selectedId={selectedCategory?.id ?? null} onSelect={setCategoryId} creation={{ groupId: group.id, type, onCreated: onCategoryCreated }} />
        {type === 'EXPENSE' && selectedCategory && (
          <BudgetNote groupId={group.id} category={selectedCategory} amount={amount} editing={operation} currency={group.currency} language={language} />
        )}
      </fieldset>

      {splitting && (
        <ExpenseSplit groupId={group.id} userId={userId} amount={amount === null ? null : Number(amount)} currency={group.currency} language={language} share={share} onChange={setShare} />
      )}

      <fieldset className="field-group" {...tourTarget('form-date')}>
        <legend className="field-label">{strings.dateLabel}</legend>
        <div className="date-chips">
          {quickDates.map((quickDate, daysAgo) => (
            <button key={quickDate} type="button" className="date-chip" aria-pressed={date === quickDate} onClick={() => setDate(quickDate)}>
              {strings.quickDate(daysAgo)}
            </button>
          ))}
          <button type="button" className="date-chip date-chip-calendar" aria-label={strings.pickDate} aria-pressed={customDate} onClick={() => setCalendarOpen(true)}>
            <CalendarIcon />
          </button>
        </div>
        {customDate && <p className="caption">{formatDayLabel(date, new Date(), language)}</p>}
      </fieldset>

      <input
        className="text-field"
        aria-label={strings.commentLabel}
        placeholder={strings.commentPlaceholder}
        maxLength={MAX_COMMENT_LENGTH - tagsLength(tags)}
        value={comment}
        onChange={(event) => setComment(event.target.value)}
      />
      <TagsField groupId={group.id} tags={tags} onChange={setTags} />
      {!operation && (
        <fieldset className="field-group" {...tourTarget('form-repeat')}>
          <legend className="field-label">{strings.repeatLabel}</legend>
          <Segmented variant="choices" label={strings.repeatLabel} options={REPEAT_CHOICES} value={repeat} optionKey={(option) => option ?? 'none'} optionLabel={repeatChoiceLabel} onChange={setRepeat} />
        </fieldset>
      )}

      {operation && (
        <button type="button" className="link-button danger-link" onClick={() => setDeleteOpen(true)}>{strings.deleteOperation}</button>
      )}
      {saveError && <p className="error-text" role="alert">{saveError}</p>}

      <BottomBar>
        {huge && !saving
          ? <HoldButton label={strings.holdToSave} disabled={selectedCategory === null} onComplete={() => void save()} />
          : (
            <Button disabled={selectedCategory === null || saving} onClick={save}>
              {saving ? strings.saving : operation ? strings.saveChanges : strings.save}
            </Button>
          )}
      </BottomBar>

      {calendarOpen && (
        <CalendarSheet value={date} max={today} language={language} onPick={(picked) => { setDate(picked); setCalendarOpen(false) }} onClose={() => setCalendarOpen(false)} />
      )}
      {deleteOpen && operation && (
        <DeleteSheet group={group} language={language} operation={operation} onDeleted={onDeleted} onClose={() => setDeleteOpen(false)} />
      )}
    </>
  )
}

function daysBefore(daysAgo: number): Date {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate() - daysAgo)
}

function tagsLength(tags: string[]): number {
  return withTags('', tags)?.length ?? 0
}
