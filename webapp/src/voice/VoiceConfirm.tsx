import { ExpenseSplit } from '../debts/ExpenseSplit'
import { shareOf } from '../debts/share'
import { useId, useState } from 'react'
import type { Category, Group, Operation, Repeat } from '../api/types'
import { CATEGORY_TYPES } from '../categories/categoryTypes'
import { useCategories } from '../categories/useCategories'
import { formatDayLabel, formatSignedMoney, toIsoDate } from '../format'
import { parseAmount } from '../operation/amount'
import { CalendarSheet } from '../operation/CalendarSheet'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'
import { type Draft, draftReady, repeatedKeys, saveDraft, withCategory, withType } from './drafts'
import { repeatChoiceLabel, REPEAT_CHOICES } from '../recurring/recurrence'
import { startRepeating } from '../recurring/repeatOperation'
import { MAX_COMMENT_LENGTH } from '../operation/limits'

type Field = 'amount' | 'category' | 'date' | 'comment'


export function VoiceConfirm({ group, userId, language, initialDrafts, onSayAgain, onClose, onSaved }: {
  group: Group
  userId: number
  language: string
  initialDrafts: Draft[]
  onSayAgain: () => void
  onClose: () => void
  onSaved: (operations: Operation[], note?: string) => void
}) {
  const [drafts, setDrafts] = useState(initialDrafts)
  const [editing, setEditing] = useState<{ key: string; field: Field } | null>(null)
  const categories = useCategories(group.id)
  const submit = useSubmit()
  const allReady = drafts.length > 0 && drafts.every(draftReady)
  const editedDraft = editing ? drafts.find((draft) => draft.key === editing.key) : undefined

  const update = (next: Draft) => setDrafts((list) => list.map((draft) => (draft.key === next.key ? next : draft)))
  const remove = (key: string) => setDrafts((list) => list.filter((draft) => draft.key !== key))
  const repeated = repeatedKeys(drafts)
  const closeField = () => setEditing(null)

  const save = async () => {
    const saved: Operation[] = []
    const created = new Map<string, Category>()
    const savedKeys = new Set<string>()
    let note: string | undefined
    const result = await submit.run(async () => {
      for (const draft of drafts) {
        const operation = await saveDraft(group.id, draft, created)
        saved.push(operation)
        savedKeys.add(draft.key)
        if (draft.repeat) note = await startRepeating(group.id, operation, draft.repeat)
      }
      return saved
    }, strings.saveFailed)
    if (result.ok) onSaved(result.value, note)
    else setDrafts((list) => list.filter((draft) => !savedKeys.has(draft.key)))
  }

  return (
    <>
      <ScreenHeader title={drafts.length > 1 ? strings.confirmManyTitle : strings.confirmOneTitle} onBack={onClose} />
      <ul className="draft-list">
        {drafts.map((draft) => (
          <li key={draft.key}>
            <DraftCard
              draft={draft}
              group={group}
              userId={userId}
              language={language}
              removable={drafts.length > 1}
              repeated={repeated.has(draft.key)}
              onChange={update}
              onEdit={(field) => setEditing({ key: draft.key, field })}
              onRemove={() => remove(draft.key)}
            />
          </li>
        ))}
      </ul>
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      <BottomBar>
        <Button variant="secondary" onClick={onSayAgain}>{strings.sayAgain}</Button>
        <Button disabled={!allReady || submit.pending} onClick={save}>
          {submit.pending ? strings.saving : drafts.length > 1 ? strings.saveCount(drafts.length) : strings.save}
        </Button>
      </BottomBar>
      {editing && editedDraft && editing.field === 'date' && (
        <CalendarSheet
          value={editedDraft.operationDate}
          max={toIsoDate(new Date())}
          language={language}
          onPick={(operationDate) => {
            update({ ...editedDraft, operationDate })
            closeField()
          }}
          onClose={closeField}
        />
      )}
      {editing && editedDraft && editing.field === 'category' && (
        <CategorySheet
          draft={editedDraft}
          categories={categories.state.status === 'ready' ? categories.state.data : []}
          onPick={(category) => {
            update(withCategory(editedDraft, category))
            closeField()
          }}
          onClose={closeField}
        />
      )}
      {editing && editedDraft && (editing.field === 'amount' || editing.field === 'comment') && (
        <TextSheet
          field={editing.field}
          draft={editedDraft}
          onDone={(next) => {
            update(next)
            closeField()
          }}
          onClose={closeField}
        />
      )}
    </>
  )
}

function DraftCard({ draft, group, userId, language, removable, repeated, onChange, onEdit, onRemove }: {
  draft: Draft
  group: Group
  userId: number
  language: string
  removable: boolean
  repeated: boolean
  onChange: (draft: Draft) => void
  onEdit: (field: Field) => void
  onRemove: () => void
}) {
  const income = draft.type === 'INCOME'
  const category = draft.categoryName ? `${draft.categoryIcon ?? ''} ${draft.categoryName}`.trim() : strings.pickCategory
  return (
    <section className="card draft-card">
      <span className="draft-head">
        <button type="button" className={`amount draft-amount ${income ? 'income' : ''}`} onClick={() => onEdit('amount')}>
          {formatSignedMoney(draft.type, draft.amount, group.currency, language)}
        </button>
        {removable && <button type="button" className="link-button danger-link" onClick={onRemove}>{strings.removeOperation}</button>}
      </span>
      {repeated && <p className="field-message warning" role="status">{strings.repeatedDraft}</p>}
      <Segmented label={strings.operationTypeLabel} options={CATEGORY_TYPES} value={draft.type} optionKey={(type) => type} optionLabel={strings.operationType} onChange={(type) => onChange(withType(draft, type))} />
      <dl className="detail-fields draft-fields">
        <FieldRow label={strings.categoryLabel} value={category} warning={!draftReady(draft) && draft.categoryId === null && !draft.isNewCategory} badge={draft.isNewCategory ? strings.newCategoryBadge : undefined} onClick={() => onEdit('category')} />
        <FieldRow label={strings.dateLabel} value={formatDayLabel(draft.operationDate, new Date(), language)} onClick={() => onEdit('date')} />
        <FieldRow label={strings.commentLabel} value={draft.description ?? strings.noComment} onClick={() => onEdit('comment')} />
        <FieldRow label={strings.repeatLabel} value={repeatChoiceLabel(draft.repeat?.unit ?? null)} onClick={() => onChange({ ...draft, repeat: nextRepeat(draft.repeat) })} />
      </dl>
      {group.splitExpenses && draft.type === 'EXPENSE' && (
        <ExpenseSplit
          groupId={group.id}
          userId={userId}
          amount={Number(draft.amount)}
          currency={group.currency}
          language={language}
          share={shareOf(draft)}
          onChange={(share) => onChange({ ...draft, payerId: share.payerId, participantIds: share.participantIds })}
        />
      )}
      {draft.isNewCategory && draft.categoryName && <p className="caption">{strings.newCategoryNote(draft.categoryName)}</p>}
    </section>
  )
}

function FieldRow({ label, value, badge, warning = false, onClick }: {
  label: string
  value: string
  badge?: string
  warning?: boolean
  onClick: () => void
}) {
  return (
    <div className="detail-field">
      <dt className="caption">{label}</dt>
      <dd>
        <button type="button" className={`field-button ${warning ? 'warning' : ''}`} aria-label={`${label}: ${value}`} onClick={onClick}>
          {value}
          {badge && <span className="badge">{badge}</span>}
        </button>
      </dd>
    </div>
  )
}

function CategorySheet({ draft, categories, onPick, onClose }: {
  draft: Draft
  categories: Category[]
  onPick: (category: Category) => void
  onClose: () => void
}) {
  const ofType = categories.filter((category) => category.type === draft.type)
  return (
    <Sheet label={strings.categoryLabel} onClose={onClose}>
      <h2 className="sheet-title">{strings.categoryLabel}</h2>
      {ofType.length === 0
        ? <p className="hint-box caption">{strings.noCategoriesOfType}</p>
        : (
          <div className="category-tiles">
            {ofType.map((category) => (
              <button key={category.id} type="button" className="category-tile" aria-pressed={category.id === draft.categoryId} onClick={() => onPick(category)}>
                <span className="category-tile-icon" aria-hidden="true">{category.icon}</span>
                <span className="category-tile-name">{category.name}</span>
              </button>
            ))}
          </div>
        )}
    </Sheet>
  )
}

function TextSheet({ field, draft, onDone, onClose }: { field: 'amount' | 'comment'; draft: Draft; onDone: (draft: Draft) => void; onClose: () => void }) {
  const amountField = field === 'amount'
  const [text, setText] = useState(amountField ? draft.amount : draft.description ?? '')
  const id = useId()
  const label = amountField ? strings.amountLabel : strings.commentLabel
  const amount = parseAmount(text)
  const invalid = amountField && amount === null

  const done = () => {
    if (amountField && amount !== null) onDone({ ...draft, amount })
    if (!amountField) onDone({ ...draft, description: text.trim() || null })
  }

  return (
    <Sheet label={label} onClose={onClose}>
      <label className="field-label" htmlFor={id}>{label}</label>
      <input
        id={id}
        className={`text-field ${invalid && text.trim() !== '' ? 'field-invalid' : ''}`}
        inputMode={amountField ? 'decimal' : 'text'}
        placeholder={amountField ? '0' : strings.commentPlaceholder}
        autoComplete="off"
        maxLength={amountField ? undefined : MAX_COMMENT_LENGTH}
        value={text}
        onChange={(event) => setText(event.target.value)}
      />
      {invalid && text.trim() !== '' && <p className="field-message negative" role="alert">{strings.amountInvalid}</p>}
      <Button disabled={invalid} onClick={done}>{strings.done}</Button>
    </Sheet>
  )
}

function nextRepeat(repeat: Repeat | null | undefined): Repeat | null {
  const next = REPEAT_CHOICES[(REPEAT_CHOICES.indexOf(repeat?.unit ?? null) + 1) % REPEAT_CHOICES.length]
  return next === null ? null : { unit: next, interval: 1 }
}
