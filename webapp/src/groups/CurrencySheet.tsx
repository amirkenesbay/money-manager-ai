import { useState } from 'react'
import type { Group } from '../api/types'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'
import { CurrencyChoices } from './CurrencyChoices'
import { updateGroup } from './groupsApi'

export function CurrencySheet({ group, onSaved, onClose }: { group: Group; onSaved: (group: Group) => void; onClose: () => void }) {
  const [currency, setCurrency] = useState(group.currency)
  const submit = useSubmit()
  const title = strings.currencySheetTitle(group.name)

  const done = async () => {
    if (currency === group.currency) {
      onClose()
      return
    }
    const result = await submit.run(() => updateGroup(group.id, { currency }), strings.saveFailed)
    if (result.ok) onSaved(result.value)
  }

  return (
    <Sheet label={title} onClose={onClose}>
      <h2 className="sheet-title">{title}</h2>
      <CurrencyChoices value={currency} disabled={!group.isOwner} onChange={setCurrency} />
      <p className={group.isOwner ? 'hint' : 'hint warning'}>{group.isOwner ? strings.currencyOwnerHint : strings.currencyNonOwnerHint}</p>
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      {group.isOwner
        ? <Button disabled={submit.pending} onClick={done}>{strings.done}</Button>
        : <Button variant="secondary" onClick={onClose}>{strings.understood}</Button>}
    </Sheet>
  )
}
