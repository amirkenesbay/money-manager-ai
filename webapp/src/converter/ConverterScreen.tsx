import { useCallback, useState } from 'react'
import type { Currency, Group } from '../api/types'
import { useAsync } from '../api/useAsync'
import { currencySymbol, formatMoney, toIsoDate } from '../format'
import { CurrencyChoices } from '../groups/CurrencyChoices'
import { AmountPad, BACKSPACE, type PadKey } from '../operation/AmountPad'
import { amountOfExpression, displayExpression, normalizeExpression } from '../operation/calc'
import { convertAmount, rateText } from '../operation/foreign'
import { fetchRate } from '../operations/operationsApi'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ChevronDownIcon } from '../ui/icons'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Sheet } from '../ui/Sheet'
import { saveCurrencies, savedCurrencies } from './converterCurrencies'
import './converter.css'

const START_AMOUNT = '100'

type Picking = 'base' | 'add'

export function ConverterScreen({ group, language, onBack, onRecord }: {
  group: Group
  language: string
  onBack: () => void
  onRecord: (currency: Currency, amount: string) => void
}) {
  const [currencies, setCurrencies] = useState(() => savedCurrencies(group.currency))
  const [base, setBase] = useState<Currency>(() => currencies.find((currency) => currency !== group.currency) ?? group.currency)
  const [amountText, setAmountText] = useState(START_AMOUNT)
  const [picking, setPicking] = useState<Picking | null>(null)
  const targets = currencies.filter((currency) => currency !== base)
  const amount = amountOfExpression(amountText)
  const rates = useRates(base, targets)

  const remember = (next: Currency[]) => {
    setCurrencies(next)
    saveCurrencies(next.filter((currency) => currency !== group.currency))
  }

  const makeBase = (currency: Currency, converted: string | null) => {
    if (!currencies.includes(currency)) remember([...currencies, currency])
    setBase(currency)
    if (converted !== null) setAmountText(converted)
  }

  const pick = (currency: Currency) => {
    if (picking === 'base') makeBase(currency, null)
    else remember([...currencies, currency])
    setPicking(null)
  }

  const press = (key: PadKey) => setAmountText((current) => (key === BACKSPACE ? current.slice(0, -1) : normalizeExpression(current + key)))

  return (
    <>
      <ScreenHeader title={strings.converterTitle} onBack={onBack} />
      <div className="amount-hero converter-hero">
        <span className="amount-hero-row">
          <output className="amount-hero-input amount" aria-label={strings.amountLabel}>{displayExpression(amountText) || '0'}</output>
          <button type="button" className="amount-hero-currency pressable" aria-label={`${strings.currencyFieldLabel}: ${base}`} onClick={() => setPicking('base')}>
            {currencySymbol(base)} <ChevronDownIcon />
          </button>
        </span>
      </div>
      <ul className="rows converter-rows">
        {targets.map((currency) => {
          const rate = rates?.[currency] ?? null
          const converted = amount !== null && rate !== null ? convertAmount(amount, rate) : null
          return (
            <li key={currency}>
              <button type="button" className="row row-button pressable converter-row" onClick={() => makeBase(currency, converted)}>
                <span className="icon-circle" aria-hidden="true">{currencySymbol(currency)}</span>
                <span className="row-main">
                  <span className="row-title">{strings.currencyName(currency)}</span>
                  <span className="caption">{rateCaption(rate, rates === null, base, currency, language)}</span>
                </span>
                <span className="amount">{converted === null ? '—' : formatMoney(converted, currency, language)}</span>
              </button>
            </li>
          )
        })}
      </ul>
      <button type="button" className="chip pressable converter-add" onClick={() => setPicking('add')}>{strings.converterAdd}</button>

      <BottomBar>
        <div className="form-keypad">
          <AmountPad onPress={press} />
          <Button disabled={amount === null} onClick={() => amount !== null && onRecord(base, amount)}>
            {amount === null ? strings.converterRecord : strings.recordAmount(formatMoney(amount, base, language))}
          </Button>
        </div>
      </BottomBar>

      {picking && (
        <Sheet label={strings.currencyFieldLabel} onClose={() => setPicking(null)}>
          <h2 className="sheet-title">{strings.currencyFieldLabel}</h2>
          <CurrencyChoices value={picking === 'base' ? base : group.currency} onChange={pick} />
        </Sheet>
      )}
    </>
  )
}

function rateCaption(rate: number | null, loading: boolean, base: Currency, target: Currency, language: string): string {
  if (rate !== null) return rateText(rate, base, target, language)
  return loading ? strings.rateLoading : strings.converterNoRate
}

function useRates(base: Currency, targets: Currency[]): Partial<Record<Currency, number>> | null {
  const key = targets.join()
  const load = useCallback(
    async (signal: AbortSignal) => {
      const today = toIsoDate(new Date())
      const entries = await Promise.all(key.split(',').filter(Boolean).map(async (target) => {
        const rate = await fetchRate(base, target as Currency, today, signal).catch(() => null)
        return [target, rate === null ? null : Number(rate.rate)] as const
      }))
      return Object.fromEntries(entries.filter(([, rate]) => rate !== null)) as Partial<Record<Currency, number>>
    },
    [base, key],
  )
  const { state } = useAsync(load)
  return state.status === 'ready' ? state.data : null
}
