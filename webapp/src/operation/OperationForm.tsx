import { useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import { serverMessage } from '../api/client'
import type { Category, CategoryType, Currency, Group, Operation, RecurrenceUnit } from '../api/types'
import { useCategories } from '../categories/useCategories'
import { currencySymbol, formatMoney, toIsoDate } from '../format'
import { createOperation, type OperationInput, replaceOperation } from '../operations/operationsApi'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { useShake } from '../ui/useShake'
import { HoldButton } from '../ui/HoldButton'
import { SkeletonRows } from '../ui/Skeleton'
import { PencilIcon } from '../ui/icons'
import { Sheet } from '../ui/Sheet'
import { HUGE_AMOUNT } from './amount'
import { AmountPad, BACKSPACE, type PadKey } from './AmountPad'
import { amountOfExpression, displayExpression, hasOperator, normalizeExpression } from './calc'
import { CalendarSheet } from './CalendarSheet'
import { dateChipLabel } from './dateChip'
import { CategorySheet, DateSheet, DetailsSheet } from './FormSheets'
import { DeleteSheet } from './DeleteSheet'
import { CurrencyChoices } from '../groups/CurrencyChoices'
import { APPROXIMATELY, convertAmount, lastCurrency, type RateChoice, rateText, rememberCurrency, rememberedRate, rememberRate } from './foreign'
import { RateSheet } from './RateSheet'
import { useExchangeRate } from './useExchangeRate'
import './operation.css'
import { startRepeating } from '../recurring/repeatOperation'
import { tagsOf, withoutTags, withTags } from '../operations/tags'
import { ExpenseSplit } from '../debts/ExpenseSplit'
import { type Share, shareOf } from '../debts/share'

const OPERATION_TYPES: CategoryType[] = ['EXPENSE', 'INCOME']
const QUICK_DAYS_AGO = [0, 1, 2]
const CATEGORY_SKELETON_ROWS = 2
const LONG_AMOUNT = 9

type FormSheet = 'category' | 'date' | 'details' | 'currency' | 'rate'

export type SaveResult = { operation: Operation; created: boolean; note?: string }

export function OperationForm({ group, userId, language, operation, initialType = 'EXPENSE', initialCategoryId, initialCurrency, initialAmount, onBack, onSaved, onDeleted }: {
  group: Group
  userId: number
  language: string
  operation: Operation | null
  initialType?: CategoryType
  initialCategoryId?: string
  initialCurrency?: Currency
  initialAmount?: string
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
        <FormFields group={group} userId={userId} language={language} operation={operation} initialType={initialType} initialCategoryId={initialCategoryId} initialCurrency={initialCurrency} initialAmount={initialAmount} categories={categories.state.data} onCategoryCreated={categories.add} onSaved={onSaved} onDeleted={onDeleted} />
      )}
    </>
  )
}

function FormFields({ group, userId, language, operation, initialType, initialCategoryId, initialCurrency, initialAmount, categories, onCategoryCreated, onSaved, onDeleted }: {
  group: Group
  userId: number
  language: string
  operation: Operation | null
  initialType: CategoryType
  initialCategoryId?: string
  initialCurrency?: Currency
  initialAmount?: string
  categories: Category[]
  onCategoryCreated: (category: Category) => void
  onSaved: (result: SaveResult) => void
  onDeleted: (operation: Operation) => void
}) {
  const today = toIsoDate(new Date())
  const [type, setType] = useState<CategoryType>(operation?.type ?? initialType)
  const [amountText, setAmountText] = useState(operation ? operation.original?.amount ?? operation.amount : initialAmount ?? '')
  const [currency, setCurrency] = useState<Currency>(() => initialCurrency ?? startingCurrency(group, operation))
  const [rateChoice, setRateChoice] = useState<RateChoice | null>(() => startingRate(group.id, operation, initialCurrency ?? startingCurrency(group, operation)))
  const [categoryId, setCategoryId] = useState<string | null>(operation?.categoryId ?? initialCategoryId ?? null)
  const [date, setDate] = useState(operation?.operationDate ?? today)
  const [comment, setComment] = useState(withoutTags(operation?.description ?? null))
  const [tags, setTags] = useState(tagsOf(operation?.description ?? null))
  const [repeat, setRepeat] = useState<RecurrenceUnit | null>(null)
  const [share, setShare] = useState<Share | null>(() => (operation ? shareOf(operation) : null))
  const [sheet, setSheet] = useState<FormSheet | null>(null)
  const [calendarOpen, setCalendarOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [attempted, setAttempted] = useState(false)
  const amountShake = useShake()

  const typeCategories = categories.filter((category) => category.type === type)
  const selectedCategory = typeCategories.find((category) => category.id === categoryId) ?? typeCategories[0] ?? null
  const typedAmount = amountOfExpression(amountText)
  const foreign = currency !== group.currency
  const officialRate = useExchangeRate(currency, group.currency, date, rateChoice === null)
  const rate = !foreign ? 1 : rateChoice?.value ?? (officialRate.status === 'ready' && officialRate.data ? Number(officialRate.data.rate) : null)
  const amount = typedAmount === null || rate === null ? null : foreign ? convertAmount(typedAmount, rate) : typedAmount
  const displayed = displayExpression(amountText)
  const amountInvalid = (attempted || amountText.trim() !== '') && typedAmount === null
  const huge = amount !== null && Number(amount) >= HUGE_AMOUNT
  const quickDates = QUICK_DAYS_AGO.map((daysAgo) => toIsoDate(daysBefore(daysAgo)))
  const splitting = group.splitExpenses && type === 'EXPENSE'
  const dirty = operation === null ? amountText.trim() !== '' || comment.trim() !== '' || tags.length > 0 : true

  useConfirmClosing(dirty)

  const selectType = (next: CategoryType) => {
    setType(next)
    setCategoryId(null)
  }

  const pickDate = (picked: string) => {
    setDate(picked)
    if (rateChoice && !rateChoice.custom) setRateChoice(null)
  }

  const pickCurrency = (picked: Currency) => {
    setCurrency(picked)
    const remembered = rememberedRate(group.id, picked)
    setRateChoice(remembered === null ? null : { value: remembered, custom: true })
    setSheet(null)
  }

  const chooseRate = (value: number | null, remember: boolean) => {
    if (value !== null) setRateChoice({ value, custom: true })
    rememberRate(group.id, currency, remember ? value : null)
  }

  const save = async () => {
    if (typedAmount === null) {
      setAttempted(true)
      amountShake.shake()
      return
    }
    if (amount === null) {
      setSheet('rate')
      return
    }
    if (selectedCategory === null) return
    const split = splitting && share ? { payerId: share.payerId, participantIds: share.participantIds } : {}
    const original = foreign ? { original: { amount: typedAmount, currency } } : {}
    const input: OperationInput = { type, amount, categoryId: selectedCategory.id, operationDate: date, description: withTags(comment, tags), ...split, ...original }
    setSaving(true)
    setSaveError(null)
    try {
      const saved = operation ? await replaceOperation(group.id, operation.id, input) : await createOperation(group.id, input)
      notifyOutcome('success')
      rememberCurrency(group.id, currency)
      const note = repeat === null ? undefined : await startRepeating(group.id, saved, { unit: repeat, interval: 1 })
      onSaved({ operation: saved, created: operation === null, note })
    } catch (error) {
      notifyOutcome('error')
      setSaveError(serverMessage(error) ?? strings.saveFailed)
      setSaving(false)
    }
  }

  const pressKey = (key: PadKey) => {
    setAmountText((current) => (key === BACKSPACE ? current.slice(0, -1) : normalizeExpression(current + key)))
  }
  const detailsCount = (comment.trim() ? 1 : 0) + tags.length + (repeat ? 1 : 0)
  const saveLabel = operation ? strings.saveChanges : amount ? strings.recordAmount(formatMoney(amount, group.currency, language)) : strings.save

  return (
    <>
      <div {...tourTarget('form-type')}>
        <Segmented label={strings.operationTypeLabel} options={OPERATION_TYPES} value={type} optionKey={(option) => option} optionLabel={strings.operationType} onChange={selectType} />
      </div>
      {!operation && <ScreenTour hint="OPERATION_FORM" />}
      <label className={`amount-hero ${amountInvalid ? 'field-invalid' : ''} ${huge ? 'field-warning' : ''} ${amountShake.shakeClass}`} onAnimationEnd={amountShake.onAnimationEnd}>
        <span className="amount-hero-row">
          <input
            className={`amount-hero-input amount ${type === 'INCOME' ? 'income' : ''} ${displayed.length > LONG_AMOUNT ? 'long' : ''}`}
            aria-label={strings.amountLabel}
            inputMode="none"
            autoComplete="off"
            placeholder="0"
            aria-invalid={amountInvalid}
            value={displayed}
            size={Math.max(displayed.length, 1)}
            onChange={(event) => setAmountText(normalizeExpression(event.target.value))}
          />
          <button type="button" className="amount-hero-currency pressable" aria-label={`${strings.currencyFieldLabel}: ${currency}`} onClick={() => setSheet('currency')}>
            {currencySymbol(currency)}
          </button>
        </span>
        {hasOperator(amountText) && typedAmount !== null && <output className="amount-hero-result">= {formatMoney(typedAmount, currency, language)}</output>}
      </label>
      {foreign && (
        <div className="amount-foreign">
          {amount !== null && <output className="amount-foreign-sum">{`${APPROXIMATELY} ${formatMoney(amount, group.currency, language)}`}</output>}
          <button type="button" className={`chip rate-chip pressable ${rateChoice?.custom ? 'custom' : ''}`} onClick={() => setSheet('rate')}>
            {rateChipLabel(rate, rateChoice, officialRate.status === 'loading', currency, group.currency, language)}
            <PencilIcon />
          </button>
        </div>
      )}
      {amountInvalid && <p className="field-message negative amount-hero-message" role="alert">{strings.amountInvalid}</p>}
      {huge && amount !== null && <p className="field-message warning amount-hero-message" role="status">{strings.amountHuge(formatMoney(amount, group.currency, language))}</p>}

      <div className="form-chips">
        <button type="button" className="chip pressable" {...tourTarget('form-categories')} aria-label={`${strings.categoryLabel}: ${selectedCategory?.name ?? ''}`} onClick={() => setSheet('category')}>
          {selectedCategory ? `${selectedCategory.icon ?? ''} ${selectedCategory.name}`.trim() : strings.categoryLabel}
        </button>
        <button type="button" className="chip pressable" {...tourTarget('form-date')} aria-label={`${strings.dateLabel}: ${dateChipLabel(date, quickDates, language)}`} onClick={() => setSheet('date')}>
          {dateChipLabel(date, quickDates, language)}
        </button>
        {splitting && (
          <ExpenseSplit groupId={group.id} splitDefault={group.splitDefault} userId={userId} amount={amount === null ? null : Number(amount)} currency={group.currency} language={language} share={share} onChange={setShare} />
        )}
        <button type="button" className="chip pressable" {...tourTarget('form-repeat')} onClick={() => setSheet('details')}>
          {detailsCount > 0 ? strings.moreDetailsCount(detailsCount) : strings.moreDetails}
        </button>
      </div>
      {saveError && <p className="error-text" role="alert">{saveError}</p>}

      <BottomBar>
        <div className="form-keypad">
          <AmountPad onPress={pressKey} />
          {huge && !saving
            ? <HoldButton label={strings.holdToSave} disabled={selectedCategory === null} onComplete={() => void save()} />
            : (
              <Button disabled={selectedCategory === null || saving} onClick={save}>
                {saving ? strings.saving : saveLabel}
              </Button>
            )}
        </div>
      </BottomBar>

      {sheet === 'category' && (
        <CategorySheet
          group={group}
          type={type}
          categories={typeCategories}
          selected={selectedCategory}
          amount={amount}
          editing={operation}
          language={language}
          onSelect={setCategoryId}
          onCreated={onCategoryCreated}
          onClose={() => setSheet(null)}
        />
      )}
      {sheet === 'date' && (
        <DateSheet value={date} quickDates={quickDates} onPick={pickDate} onOpenCalendar={() => { setSheet(null); setCalendarOpen(true) }} onClose={() => setSheet(null)} />
      )}
      {sheet === 'details' && (
        <DetailsSheet
          groupId={group.id}
          comment={comment}
          tags={tags}
          repeat={repeat}
          canRepeat={operation === null}
          canDelete={operation !== null}
          onComment={setComment}
          onTags={setTags}
          onRepeat={setRepeat}
          onDelete={() => { setSheet(null); setDeleteOpen(true) }}
          onClose={() => setSheet(null)}
        />
      )}
      {sheet === 'currency' && (
        <Sheet label={strings.currencyFieldLabel} onClose={() => setSheet(null)}>
          <h2 className="sheet-title">{strings.currencyFieldLabel}</h2>
          <CurrencyChoices value={currency} onChange={pickCurrency} />
        </Sheet>
      )}
      {sheet === 'rate' && (
        <RateSheet
          from={currency}
          to={group.currency}
          amount={typedAmount}
          rate={rate}
          custom={rateChoice?.custom ?? false}
          remembered={rememberedRate(group.id, currency) !== null}
          language={language}
          onDone={chooseRate}
          onReset={() => setRateChoice(null)}
          onClose={() => setSheet(null)}
        />
      )}
      {calendarOpen && (
        <CalendarSheet value={date} max={today} language={language} onPick={(picked) => { pickDate(picked); setCalendarOpen(false) }} onClose={() => setCalendarOpen(false)} />
      )}
      {deleteOpen && operation && (
        <DeleteSheet group={group} language={language} operation={operation} onDeleted={onDeleted} onClose={() => setDeleteOpen(false)} />
      )}
    </>
  )
}

function startingCurrency(group: Group, operation: Operation | null): Currency {
  if (operation) return operation.original?.currency ?? group.currency
  return lastCurrency(group.id) ?? group.currency
}

function startingRate(groupId: string, operation: Operation | null, currency: Currency): RateChoice | null {
  if (operation?.original) return { value: Number(operation.amount) / Number(operation.original.amount), custom: false }
  if (operation) return null
  const remembered = rememberedRate(groupId, currency)
  return remembered === null ? null : { value: remembered, custom: true }
}

function rateChipLabel(rate: number | null, choice: RateChoice | null, loading: boolean, from: Currency, to: Currency, language: string): string {
  if (rate === null) return loading ? strings.rateLoading : strings.rateMissing
  const text = rateText(rate, from, to, language)
  return choice?.custom ? `${text} · ${strings.rateCustom}` : text
}

function daysBefore(daysAgo: number): Date {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate() - daysAgo)
}
