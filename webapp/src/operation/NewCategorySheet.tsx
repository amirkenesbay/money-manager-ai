import { useState } from 'react'
import type { Category, CategoryType } from '../api/types'
import { createCategory } from '../categories/categoriesApi'
import { ICON_PRESETS, MAX_CATEGORY_NAME_LENGTH } from '../categories/categoryTypes'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { EmojiPicker } from '../ui/EmojiPicker'
import { NameField } from '../ui/NameField'
import { Sheet } from '../ui/Sheet'
import { useNameInput } from '../ui/useNameInput'
import { useSubmit } from '../ui/useSubmit'

export function NewCategorySheet({ groupId, type, onCreated, onClose }: {
  groupId: string
  type: CategoryType
  onCreated: (category: Category) => void
  onClose: () => void
}) {
  const [icon, setIcon] = useState(ICON_PRESETS[type][0])
  const submit = useSubmit()
  const name = useNameInput('', submit.clearError)

  const save = async () => {
    const trimmed = name.require()
    if (trimmed === null) return
    const result = await submit.run(() => createCategory(groupId, { name: trimmed, icon, type }), strings.saveFailed)
    if (result.ok) onCreated(result.value)
  }

  return (
    <Sheet label={strings.newCategoryTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.newCategoryTitle}</h2>
      <NameField
        label={strings.nameLabel}
        value={name.value}
        placeholder={strings.categoryNamePlaceholder}
        maxLength={MAX_CATEGORY_NAME_LENGTH}
        error={name.missingError ?? submit.error?.message ?? null}
        onChange={name.change}
      />
      <EmojiPicker value={icon} presets={ICON_PRESETS[type]} onChange={setIcon} />
      <Button disabled={submit.pending} onClick={save}>{submit.pending ? strings.saving : strings.save}</Button>
    </Sheet>
  )
}
