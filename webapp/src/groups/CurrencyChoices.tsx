import type { Currency } from '../api/types'
import { currencySymbol } from '../format'
import { strings } from '../strings'
import { CURRENCIES } from './currencies'

export function CurrencyChoices({ value, disabled = false, onChange }: { value: Currency; disabled?: boolean; onChange: (currency: Currency) => void }) {
  return (
    <div className="currency-grid" role="radiogroup" aria-label={strings.currencyFieldLabel}>
      {CURRENCIES.map((currency) => (
        <button
          key={currency}
          type="button"
          role="radio"
          className="currency-tile"
          aria-checked={currency === value}
          aria-label={`${strings.currencyName(currency)} ${currency}`}
          disabled={disabled}
          onClick={() => onChange(currency)}
        >
          <span className="currency-symbol" aria-hidden="true">{currencySymbol(currency)}</span>
          <span aria-hidden="true">{currency}</span>
        </button>
      ))}
    </div>
  )
}
