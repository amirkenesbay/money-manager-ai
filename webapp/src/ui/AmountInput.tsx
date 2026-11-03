import { useId } from 'react'
import type { Currency } from '../api/types'
import { currencySymbol } from '../format'
import { strings } from '../strings'

export function AmountInput({ label, value, currency, invalid, onChange }: {
  label: string
  value: string
  currency: Currency
  invalid: boolean
  onChange: (value: string) => void
}) {
  const id = useId()
  return (
    <>
      <label className={`amount-field ${invalid ? 'field-invalid' : ''}`} htmlFor={id}>
        <span className="field-label">{label}</span>
        <span className="amount-input-row">
          <input id={id} className="amount-input amount" inputMode="decimal" autoComplete="off" placeholder="0" aria-invalid={invalid} value={value} onChange={(event) => onChange(event.target.value)} />
          <span className="amount-currency" aria-hidden="true">{currencySymbol(currency)}</span>
        </span>
      </label>
      {invalid && <p className="field-message negative" role="alert">{strings.amountInvalid}</p>}
    </>
  )
}
