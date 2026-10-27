import { useState } from 'react'
import type { Currency, Group } from '../api/types'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { NameField } from '../ui/NameField'
import { ScreenHeader } from '../ui/ScreenHeader'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { useNameInput } from '../ui/useNameInput'
import { useSubmit } from '../ui/useSubmit'
import { CurrencyChoices } from './CurrencyChoices'
import { createGroup } from './groupsApi'

export const MAX_GROUP_NAME_LENGTH = 50

export function GroupForm({ defaultCurrency, onBack, onCreated, onLimit }: {
  defaultCurrency: Currency
  onBack: () => void
  onCreated: (group: Group) => void
  onLimit: () => void
}) {
  const [currency, setCurrency] = useState(defaultCurrency)
  const submit = useSubmit()
  const name = useNameInput('', submit.clearError)

  useConfirmClosing(name.value.trim() !== '')

  const save = async () => {
    const trimmed = name.require()
    if (trimmed === null) return
    const result = await submit.run(() => createGroup(trimmed, currency), strings.saveFailed)
    if (result.ok) onCreated(result.value)
    else if (result.error.code === 'GROUP_LIMIT_REACHED') onLimit()
  }

  const nameError = name.missingError ?? (submit.error?.code === 'GROUP_DUPLICATE' ? submit.error.message : null)
  const otherError = submit.error && submit.error.code !== 'GROUP_DUPLICATE' ? submit.error.message : null

  return (
    <>
      <ScreenHeader title={strings.newGroupTitle} onBack={onBack} />
      <NameField label={strings.nameLabel} value={name.value} placeholder={strings.groupTemplates[0]} maxLength={MAX_GROUP_NAME_LENGTH} error={nameError} onChange={name.change} />
      <fieldset className="field-group">
        <legend className="field-label">{strings.quickPickLabel}</legend>
        <div className="template-chips">
          {strings.groupTemplates.map((template) => (
            <button key={template} type="button" className="template-chip pressable" onClick={() => name.change(template)}>{template}</button>
          ))}
        </div>
      </fieldset>
      <fieldset className="field-group">
        <legend className="field-label">{strings.currencyFieldLabel}</legend>
        <CurrencyChoices value={currency} onChange={setCurrency} />
        <p className="caption">{strings.currencyFieldHint}</p>
      </fieldset>
      <p className="hint-box caption">{strings.groupCreateHint}</p>
      {otherError && <p className="error-text" role="alert">{otherError}</p>}
      <BottomBar>
        <Button disabled={submit.pending} onClick={save}>{submit.pending ? strings.saving : strings.createGroup}</Button>
      </BottomBar>
    </>
  )
}
