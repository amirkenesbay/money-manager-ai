import { useState } from 'react'
import type { Currency } from '../api/types'
import { currencySymbol, formatMoney } from '../format'
import { parseAmount } from '../operation/amount'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'

export function AmountSheet({ title, max, currency, language, onSave, onClose }: {
  title: string
  max: string
  currency: Currency
  language: string
  onSave: (amount: string) => void
  onClose: () => void
}) {
  const [text, setText] = useState(max)
  const amount = parseAmount(text)
  const tooMuch = amount !== null && Number(amount) > Number(max)
  return (
    <Sheet label={title} onClose={onClose}>
      <h2 className="sheet-title">{title}</h2>
      <label className="amount-field">
        <span className="amount-input-row">
          <input className="amount-input amount" inputMode="decimal" autoComplete="off" aria-label={title} value={text} onChange={(event) => setText(event.target.value)} />
          <span className="amount-currency" aria-hidden="true">{currencySymbol(currency)}</span>
        </span>
      </label>
      <div className="split-quick">
        <button type="button" className="chip pressable" onClick={() => setText(max)}>{strings.loanAll(formatMoney(max, currency, language))}</button>
      </div>
      <Button disabled={amount === null || tooMuch} onClick={() => amount && onSave(amount)}>{strings.save}</Button>
      <Button variant="secondary" onClick={onClose}>{strings.cancel}</Button>
    </Sheet>
  )
}
