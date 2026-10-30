import { useState } from 'react'
import type { Group } from '../api/types'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Segmented } from '../ui/Segmented'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'
import { updateGroup } from './groupsApi'

const LAST_START_DAY = 28
const START_DAYS = Array.from({ length: LAST_START_DAY }, (_, index) => index + 1)

export function MonthStartSheet({ group, onSaved, onClose }: { group: Group; onSaved: (group: Group) => void; onClose: () => void }) {
  const [day, setDay] = useState(group.monthStartDay)
  const submit = useSubmit()

  const done = async () => {
    if (day === group.monthStartDay) {
      onClose()
      return
    }
    const result = await submit.run(() => updateGroup(group.id, { monthStartDay: day }), strings.saveFailed)
    if (result.ok) onSaved(result.value)
  }

  return (
    <Sheet label={strings.monthStartSheetTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.monthStartSheetTitle}</h2>
      <Segmented
        variant="choices"
        label={strings.monthStartSheetTitle}
        options={START_DAYS}
        value={day}
        optionKey={String}
        optionLabel={String}
        onChange={(next) => group.isOwner && setDay(next)}
      />
      <p className={group.isOwner ? 'hint' : 'hint warning'}>{group.isOwner ? strings.monthStartOwnerHint : strings.monthStartNonOwnerHint}</p>
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      {group.isOwner
        ? <Button disabled={submit.pending} onClick={done}>{strings.done}</Button>
        : <Button variant="secondary" onClick={onClose}>{strings.understood}</Button>}
    </Sheet>
  )
}
