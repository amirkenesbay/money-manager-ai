import { useState } from 'react'
import type { Category, CategoryType, Group } from '../api/types'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ConfirmSheet } from '../ui/ConfirmSheet'
import { EmojiPicker } from '../ui/EmojiPicker'
import { NameField } from '../ui/NameField'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { useNameInput } from '../ui/useNameInput'
import { useSubmit } from '../ui/useSubmit'
import { createCategory, deleteCategory, updateCategory } from './categoriesApi'
import { CATEGORY_TYPES } from './categoryTypes'

const MAX_CATEGORY_NAME_LENGTH = 50

const ICON_PRESETS: Record<CategoryType, string[]> = {
  EXPENSE: ['🛒', '🍔', '☕', '🚕', '🚗', '🏠', '💊', '👕', '📚', '🎬', '✈️', '📱', '🐾', '🎁', '💡', '📦'],
  INCOME: ['💰', '💼', '🎁', '📈', '🏦', '💵', '🤝', '📦'],
}

export function CategoryForm({ group, type, category, onBack, onSaved, onDeleted, onLimit }: {
  group: Group
  type: CategoryType
  category: Category | null
  onBack: () => void
  onSaved: (category: Category, created: boolean) => void
  onDeleted: (category: Category) => void
  onLimit: () => void
}) {
  const [formType, setFormType] = useState(category?.type ?? type)
  const [icon, setIcon] = useState(category?.icon ?? ICON_PRESETS[formType][0])
  const [deleteOpen, setDeleteOpen] = useState(false)
  const submit = useSubmit()
  const name = useNameInput(category?.name ?? '', submit.clearError)
  const shared = group.type === 'SHARED'
  const dirty = name.value !== (category?.name ?? '') || (category !== null && icon !== category.icon)

  useConfirmClosing(dirty)

  const save = async () => {
    const trimmed = name.require()
    if (trimmed === null) return
    const input = { name: trimmed, icon }
    const result = await submit.run(
      () => (category ? updateCategory(group.id, category.id, input) : createCategory(group.id, { ...input, type: formType })),
      strings.saveFailed,
    )
    if (result.ok) onSaved(result.value, category === null)
    else if (result.error.code === 'CATEGORY_LIMIT_REACHED') onLimit()
  }

  const nameError = name.missingError ?? (submit.error?.code === 'CATEGORY_DUPLICATE' ? submit.error.message : null)
  const otherError = submit.error && submit.error.code !== 'CATEGORY_DUPLICATE' ? submit.error.message : null

  return (
    <>
      <ScreenHeader title={category ? strings.editCategoryTitle : strings.newCategoryTitle} onBack={onBack} />
      {category
        ? <p className="hint-box caption">{strings.categoryTypeLocked}</p>
        : <Segmented label={strings.operationTypeLabel} options={CATEGORY_TYPES} value={formType} optionKey={(option) => option} optionLabel={strings.operationType} onChange={setFormType} />}
      <NameField
        label={strings.nameLabel}
        value={name.value}
        placeholder={strings.categoryNamePlaceholder}
        maxLength={MAX_CATEGORY_NAME_LENGTH}
        error={nameError}
        onChange={name.change}
      />
      <EmojiPicker value={icon} presets={ICON_PRESETS[formType]} onChange={setIcon} />
      {category && shared && <p className="hint-box caption">{strings.sharedChangeNotice}</p>}
      {category && <button type="button" className="link-button danger-link" onClick={() => setDeleteOpen(true)}>{strings.deleteCategory}</button>}
      {otherError && <p className="error-text" role="alert">{otherError}</p>}
      <BottomBar>
        <Button disabled={submit.pending} onClick={save}>{submit.pending ? strings.saving : category ? strings.saveChanges : strings.save}</Button>
      </BottomBar>
      {deleteOpen && category && (
        <ConfirmSheet
          title={strings.deleteCategoryQuestion(category.name)}
          confirmLabel={strings.delete}
          onConfirm={() => deleteCategory(group.id, category.id)}
          onDone={() => onDeleted(category)}
          onClose={() => setDeleteOpen(false)}
        >
          <p className="hint">{strings.deleteCategoryWarning}</p>
          {shared && <p className="hint">{strings.sharedChangeNotice}</p>}
        </ConfirmSheet>
      )}
    </>
  )
}
