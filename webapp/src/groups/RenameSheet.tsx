import type { Group } from '../api/types'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { NameField } from '../ui/NameField'
import { Sheet } from '../ui/Sheet'
import { useNameInput } from '../ui/useNameInput'
import { useSubmit } from '../ui/useSubmit'
import { updateGroup } from './groupsApi'
import { MAX_GROUP_NAME_LENGTH } from './GroupForm'

export function RenameSheet({ group, onSaved, onClose }: { group: Group; onSaved: (group: Group) => void; onClose: () => void }) {
  const submit = useSubmit()
  const name = useNameInput(group.name, submit.clearError)

  const save = async () => {
    const trimmed = name.require()
    if (trimmed === null) return
    const result = await submit.run(() => updateGroup(group.id, { name: trimmed }), strings.saveFailed)
    if (result.ok) onSaved(result.value)
  }

  return (
    <Sheet label={strings.renameGroup} onClose={onClose}>
      <h2 className="sheet-title">{strings.renameGroup}</h2>
      <NameField
        label={strings.nameLabel}
        value={name.value}
        placeholder={group.name}
        maxLength={MAX_GROUP_NAME_LENGTH}
        error={name.missingError ?? submit.error?.message ?? null}
        onChange={name.change}
      />
      <Button disabled={submit.pending} onClick={save}>{submit.pending ? strings.saving : strings.save}</Button>
      <Button variant="secondary" onClick={onClose}>{strings.keep}</Button>
    </Sheet>
  )
}
