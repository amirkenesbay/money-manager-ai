import type { Category, CategoryType, Group, Operation, RecurrenceUnit } from '../api/types'
import { BudgetNote } from '../budgets/BudgetNote'
import { repeatChoiceLabel, REPEAT_CHOICES } from '../recurring/recurrence'
import { withTags } from '../operations/tags'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { CalendarIcon } from '../ui/icons'
import { Segmented } from '../ui/Segmented'
import { Sheet } from '../ui/Sheet'
import { CategoryTiles } from './CategoryTiles'
import { MAX_COMMENT_LENGTH } from './limits'
import { TagsField } from './TagsField'

export function CategorySheet({ group, type, categories, selected, amount, editing, language, onSelect, onCreated, onClose }: {
  group: Group
  type: CategoryType
  categories: Category[]
  selected: Category | null
  amount: string | null
  editing: Operation | null
  language: string
  onSelect: (categoryId: string) => void
  onCreated: (category: Category) => void
  onClose: () => void
}) {
  return (
    <Sheet label={strings.categoryLabel} onClose={onClose}>
      <h2 className="sheet-title">{strings.categoryLabel}</h2>
      <CategoryTiles
        categories={categories}
        selectedId={selected?.id ?? null}
        onSelect={(categoryId) => { onSelect(categoryId); onClose() }}
        creation={{ groupId: group.id, type, onCreated }}
      />
      {type === 'EXPENSE' && selected && (
        <BudgetNote groupId={group.id} category={selected} amount={amount} editing={editing} currency={group.currency} language={language} />
      )}
    </Sheet>
  )
}

export function DateSheet({ value, quickDates, onPick, onOpenCalendar, onClose }: {
  value: string
  quickDates: string[]
  onPick: (date: string) => void
  onOpenCalendar: () => void
  onClose: () => void
}) {
  return (
    <Sheet label={strings.dateLabel} onClose={onClose}>
      <h2 className="sheet-title">{strings.dateLabel}</h2>
      <div className="date-chips">
        {quickDates.map((quickDate, daysAgo) => (
          <button key={quickDate} type="button" className="date-chip" aria-pressed={value === quickDate} onClick={() => { onPick(quickDate); onClose() }}>
            {strings.quickDate(daysAgo)}
          </button>
        ))}
        <button type="button" className="date-chip date-chip-calendar" aria-label={strings.pickDate} aria-pressed={!quickDates.includes(value)} onClick={onOpenCalendar}>
          <CalendarIcon />
        </button>
      </div>
    </Sheet>
  )
}

export function DetailsSheet({ groupId, comment, tags, repeat, canRepeat, canDelete, onComment, onTags, onRepeat, onDelete, onClose }: {
  groupId: string
  comment: string
  tags: string[]
  repeat: RecurrenceUnit | null
  canRepeat: boolean
  canDelete: boolean
  onComment: (comment: string) => void
  onTags: (tags: string[]) => void
  onRepeat: (repeat: RecurrenceUnit | null) => void
  onDelete: () => void
  onClose: () => void
}) {
  return (
    <Sheet label={strings.moreDetails} onClose={onClose}>
      <h2 className="sheet-title">{strings.moreDetails}</h2>
      <input
        className="text-field"
        aria-label={strings.commentLabel}
        placeholder={strings.commentPlaceholder}
        maxLength={MAX_COMMENT_LENGTH - tagsLength(tags)}
        value={comment}
        onChange={(event) => onComment(event.target.value)}
      />
      <TagsField groupId={groupId} tags={tags} onChange={onTags} />
      {canRepeat && (
        <fieldset className="field-group">
          <legend className="field-label">{strings.repeatLabel}</legend>
          <Segmented variant="choices" label={strings.repeatLabel} options={REPEAT_CHOICES} value={repeat} optionKey={(option) => option ?? 'none'} optionLabel={repeatChoiceLabel} onChange={onRepeat} />
        </fieldset>
      )}
      {canDelete && <button type="button" className="link-button danger-link" onClick={onDelete}>{strings.deleteOperation}</button>}
      <Button onClick={onClose}>{strings.done}</Button>
    </Sheet>
  )
}

function tagsLength(tags: string[]): number {
  return withTags('', tags)?.length ?? 0
}
