import { useState } from 'react'
import type { Currency } from '../api/types'
import { CURRENCIES } from '../currencies'
import { currencySymbol } from '../format'
import { strings } from '../strings'

export function CurrencyChoices({ value, disabled = false, onChange }: { value: Currency; disabled?: boolean; onChange: (currency: Currency) => void }) {
  const [query, setQuery] = useState('')
  const needle = query.trim().toLocaleLowerCase()
  const found = CURRENCIES.filter((currency) => matches(currency, needle))
  return (
    <div className="currency-picker">
      <input
        className="text-field"
        type="search"
        aria-label={strings.currencySearchLabel}
        placeholder={strings.currencySearchLabel}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      {found.length === 0 && <p className="caption">{strings.iconSearchEmpty}</p>}
      <div className="currency-list" role="radiogroup" aria-label={strings.currencyFieldLabel}>
        {found.map((currency) => (
          <button
            key={currency}
            type="button"
            role="radio"
            className="currency-row"
            aria-checked={currency === value}
            aria-label={`${strings.currencyName(currency)} ${currency}`}
            disabled={disabled}
            onClick={() => onChange(currency)}
          >
            <span className="currency-symbol" aria-hidden="true">{currencySymbol(currency)}</span>
            <span className="currency-name" aria-hidden="true">{strings.currencyName(currency)}</span>
            <span className="caption" aria-hidden="true">{currency}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

function matches(currency: Currency, needle: string): boolean {
  if (needle === '') return true
  return [currency, strings.currencyName(currency), currencySymbol(currency)].some((text) => text.toLocaleLowerCase().includes(needle))
}
