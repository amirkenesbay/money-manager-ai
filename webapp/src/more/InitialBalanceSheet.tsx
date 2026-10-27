import { useId, useState } from 'react'
import type { Group } from '../api/types'
import { currencySymbol } from '../format'
import { setInitialBalance } from '../groups/groupsApi'
import { parseBalance } from '../operation/amount'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'

export function InitialBalanceSheet({ group, onSaved, onClose }: { group: Group; onSaved: (group: Group) => void; onClose: () => void }) {
  const [text, setText] = useState(group.initialBalance)
  const submit = useSubmit()
  const id = useId()
  const amount = parseBalance(text)
  const invalid = text.trim() !== '' && amount === null

  const save = async () => {
    if (amount === null) return
    const result = await submit.run(() => setInitialBalance(group.id, amount), strings.saveFailed)
    if (result.ok) onSaved(result.value)
  }

  return (
    <Sheet label={strings.initialBalanceLabel} onClose={onClose}>
      <h2 className="sheet-title">{strings.initialBalanceLabel}</h2>
      <p className="hint">{strings.initialBalanceHint}</p>
      <label className={`amount-field ${invalid ? 'field-invalid' : ''}`} htmlFor={id}>
        <span className="field-label">{group.name}</span>
        <span className="amount-input-row">
          <input id={id} className="amount-input amount" inputMode="decimal" autoComplete="off" placeholder="0" aria-invalid={invalid} value={text} onChange={(event) => setText(event.target.value)} />
          <span className="amount-currency" aria-hidden="true">{currencySymbol(group.currency)}</span>
        </span>
      </label>
      {invalid && <p className="field-message negative" role="alert">{strings.amountInvalid}</p>}
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      <Button disabled={amount === null || submit.pending} onClick={save}>{submit.pending ? strings.saving : strings.save}</Button>
    </Sheet>
  )
}
