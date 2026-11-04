import { useState } from 'react'
import { serverMessage } from '../api/client'
import type { Category, CategoryType, Group, Operation } from '../api/types'
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
import { SkeletonRows } from '../ui/Skeleton'
import { HUGE_AMOUNT, parseAmount } from './amount'
import { CalendarSheet } from './CalendarSheet'
import { DeleteSheet } from './DeleteSheet'
import './operation.css'
import { MAX_COMMENT_LENGTH } from './limits'
import { TagsField } from './TagsField'
import { tagsOf, withoutTags, withTags } from '../operations/tags'
import { Hint } from '../onboarding/Hint'
import { BudgetNote } from '../budgets/BudgetNote'

const OPERATION_TYPES: CategoryType[] = ['EXPENSE', 'INCOME']
const QUICK_DAYS_AGO = [0, 1, 2]
const CATEGORY_SKELETON_ROWS = 2

export type SaveResult = { operation: Operation; created: boolean }

export function OperationForm({ group, language, operation, initialType = 'EXPENSE', onBack, onSaved, onDeleted }: {
  group: Group
  language: string
  operation: Operation | null
  initialType?: CategoryType
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
        <FormFields group={group} language={language} operation={operation} initialType={initialType} categories={categories.state.data} onSaved={onSaved} onDeleted={onDeleted} />
      )}
    </>
  )
}

function FormFields({ group, language, operation, initialType, categories, onSaved, onDeleted }: {
  group: Group
  language: string
  operation: Operation | null
  initialType: CategoryType
  categories: Category[]
  onSaved: (result: SaveResult) => void
  onDeleted: (operation: Operation) => void
}) {
  const today = toIsoDate(new Date())
  const [type, setType] = useState<CategoryType>(operation?.type ?? initialType)
  const [amountText, setAmountText] = useState(operation ? operation.amount : '')
  const [categoryId, setCategoryId] = useState<string | null>(operation?.categoryId ?? null)
  const [date, setDate] = useState(operation?.operationDate ?? today)
  const [comment, setComment] = useState(withoutTags(operation?.description ?? null))
  const [tags, setTags] = useState(tagsOf(operation?.description ?? null))
  const [calendarOpen, setCalendarOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)

  const typeCategories = categories.filter((category) => category.type === type)
  const selectedCategory = typeCategories.find((category) => category.id === categoryId) ?? typeCategories[0] ?? null
  const amount = parseAmount(amountText)
  const amountInvalid = amountText.trim() !== '' && amount === null
  const huge = amount !== null && Number(amount) >= HUGE_AMOUNT
  const quickDates = QUICK_DAYS_AGO.map((daysAgo) => toIsoDate(daysBefore(daysAgo)))
  const customDate = !quickDates.includes(date)
  const dirty = operation === null ? amountText.trim() !== '' || comment.trim() !== '' || tags.length > 0 : true

  useConfirmClosing(dirty)

  const selectType = (next: CategoryType) => {
    setType(next)
    setCategoryId(null)
  }

  const save = async () => {
    if (amount === null || selectedCategory === null) return
    const input: OperationInput = { type, amount, categoryId: selectedCategory.id, operationDate: date, description: withTags(comment, tags) }
    setSaving(true)
    setSaveError(null)
    try {
      const saved = operation ? await replaceOperation(group.id, operation.id, input) : await createOperation(group.id, input)
      notifyOutcome('success')
      onSaved({ operation: saved, created: operation === null })
    } catch (error) {
      notifyOutcome('error')
      setSaveError(serverMessage(error) ?? strings.saveFailed)
      setSaving(false)
    }
  }

  return (
    <>
      <Segmented label={strings.operationTypeLabel} options={OPERATION_TYPES} value={type} optionKey={(option) => option} optionLabel={strings.operationType} onChange={selectType} />
      <label className={`amount-field ${amountInvalid ? 'field-invalid' : ''} ${huge ? 'field-warning' : ''}`}>
        <span className="field-label">{strings.amountLabel}</span>
        <span className="amount-input-row">
          <input
            className={`amount-input amount ${type === 'INCOME' ? 'income' : ''}`}
            inputMode="decimal"
            autoComplete="off"
            placeholder="0"
            aria-invalid={amountInvalid}
            value={amountText}
            onChange={(event) => setAmountText(event.target.value)}
          />
          <span className="amount-currency" aria-hidden="true">{currencySymbol(group.currency)}</span>
        </span>
      </label>
      {amountInvalid && <p className="field-message negative" role="alert">{strings.amountInvalid}</p>}
      {huge && amount !== null && <p className="field-message warning" role="status">{strings.amountHuge(formatMoney(amount, group.currency, language))}</p>}

      <fieldset className="field-group">
        <legend className="field-label">{strings.categoryLabel}</legend>
        {typeCategories.length === 0
          ? <p className="hint-box caption">{strings.noCategoriesOfType}</p>
          : (
            <div className="category-tiles">
              {typeCategories.map((category) => (
                <button key={category.id} type="button" className="category-tile" aria-pressed={category.id === selectedCategory?.id} onClick={() => setCategoryId(category.id)}>
                  <span className="category-tile-icon" aria-hidden="true">{category.icon}</span>
                  <span className="category-tile-name">{category.name}</span>
                </button>
              ))}
            </div>
          )}
        {type === 'EXPENSE' && selectedCategory && (
          <BudgetNote groupId={group.id} category={selectedCategory} amount={amount} editing={operation} currency={group.currency} language={language} />
        )}
      </fieldset>

      <fieldset className="field-group">
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
        {!operation && <Hint hint="OPERATION_FORM" title={strings.hintFormTitle} body={strings.hintFormBody} />}
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

      {operation && (
        <button type="button" className="link-button danger-link" onClick={() => setDeleteOpen(true)}>{strings.deleteOperation}</button>
      )}
      {saveError && <p className="error-text" role="alert">{saveError}</p>}

      <BottomBar>
        <Button disabled={amount === null || selectedCategory === null || saving} onClick={save}>
          {saving ? strings.saving : huge ? strings.saveHuge : operation ? strings.saveChanges : strings.save}
        </Button>
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
