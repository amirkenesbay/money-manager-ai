import { useCallback, useState } from 'react'
import type { Currency, Group } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchBalance } from '../dashboard/dashboardApi'
import { currencySymbol, formatMoney, toIsoDate } from '../format'
import { convertAmount, rateText } from '../operation/foreign'
import { fetchRate } from '../operations/operationsApi'
import { strings } from '../strings'
import type { ToastMessage } from '../ui/Toast'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'
import { CurrencyChoices } from './CurrencyChoices'
import { updateGroup } from './groupsApi'

export type UndoAction = ToastMessage['action']

export function CurrencySheet({ group, language, onSaved, onClose }: {
  group: Group
  language: string
  onSaved: (group: Group, undo?: UndoAction) => void
  onClose: () => void
}) {
  const [currency, setCurrency] = useState(group.currency)
  const [confirming, setConfirming] = useState(false)
  const submit = useSubmit()
  const title = strings.currencySheetTitle(group.name)

  const switchCurrency = async () => {
    const result = await submit.run(() => updateGroup(group.id, { currency }), strings.saveFailed)
    if (!result.ok) return
    const undo = { label: strings.currencyUndo(currencySymbol(group.currency)), onClick: () => void updateGroup(result.value.id, { currency: group.currency }).then((back) => onSaved(back)) }
    onSaved(result.value, undo)
  }

  const done = () => {
    if (currency === group.currency) onClose()
    else setConfirming(true)
  }

  if (confirming) {
    return (
      <Sheet label={title} onClose={onClose}>
        <SwitchPreview group={group} to={currency} language={language} />
        {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
        <Button disabled={submit.pending} onClick={switchCurrency}>{strings.currencySwitchTo(currencySymbol(currency))}</Button>
        <Button variant="secondary" onClick={() => setConfirming(false)}>{strings.currencyKeep(currencySymbol(group.currency))}</Button>
      </Sheet>
    )
  }

  return (
    <Sheet label={title} onClose={onClose}>
      <h2 className="sheet-title">{title}</h2>
      <CurrencyChoices value={currency} disabled={!group.isOwner} onChange={setCurrency} />
      <p className={group.isOwner ? 'hint' : 'hint warning'}>{group.isOwner ? strings.currencyOwnerHint : strings.currencyNonOwnerHint}</p>
      {group.isOwner
        ? <Button onClick={done}>{strings.done}</Button>
        : <Button variant="secondary" onClick={onClose}>{strings.understood}</Button>}
    </Sheet>
  )
}

function SwitchPreview({ group, to, language }: { group: Group; to: Currency; language: string }) {
  const load = useCallback(
    async (signal: AbortSignal) => {
      const [balance, rate] = await Promise.all([fetchBalance(group.id, signal), fetchRate(group.currency, to, toIsoDate(new Date()), signal)])
      return { total: balance.total, rate: Number(rate.rate) }
    },
    [group.id, group.currency, to],
  )
  const { state } = useAsync(load)
  if (state.status !== 'ready') return <p className="switch-preview caption">{state.status === 'error' ? state.message ?? strings.rateMissing : strings.rateLoading}</p>
  const { total, rate } = state.data
  return (
    <div className="switch-preview">
      <span className="switch-from">{formatMoney(total, group.currency, language)}</span>
      <span className="switch-arrow" aria-hidden="true">↓</span>
      <span className="switch-to amount">{formatMoney(convertAmount(total, rate), to, language)}</span>
      <span className="caption">{rateText(rate, group.currency, to, language)}</span>
    </div>
  )
}
