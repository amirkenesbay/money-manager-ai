import { useState } from 'react'
import type { Currency } from '../api/types'
import { currencySymbol } from '../format'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { Switch } from '../ui/Switch'
import { parseAmount } from './amount'
import { convertAmount, parseRate, rateInputText } from './foreign'

export function RateSheet({ from, to, amount, rate, custom, remembered, language, onDone, onReset, onClose }: {
  from: Currency
  to: Currency
  amount: string | null
  rate: number | null
  custom: boolean
  remembered: boolean
  language: string
  onDone: (rate: number | null, remember: boolean) => void
  onReset: () => void
  onClose: () => void
}) {
  const [rateText, setRateText] = useState(rate === null ? '' : rateInputText(rate, language))
  const [totalText, setTotalText] = useState(rate !== null && amount !== null ? rateInputText(Number(convertAmount(amount, rate)), language) : '')
  const [remember, setRemember] = useState(remembered)
  const typedRate = parseRate(rateText)

  const changeRate = (text: string) => {
    setRateText(text)
    const parsed = parseRate(text)
    if (parsed !== null && amount !== null) setTotalText(rateInputText(Number(convertAmount(amount, parsed)), language))
  }

  const changeTotal = (text: string) => {
    setTotalText(text)
    const parsed = parseAmount(text)
    if (parsed !== null && amount !== null) setRateText(rateInputText(Number(parsed) / Number(amount), language))
  }

  const done = () => {
    onDone(typedRate, remember)
    onClose()
  }

  return (
    <Sheet label={strings.rateTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.rateTitle}</h2>
      <label className="rate-line">
        <span>{`1 ${currencySymbol(from)} =`}</span>
        <input className="rate-input" inputMode="decimal" autoComplete="off" aria-label={strings.rateTitle} value={rateText} onChange={(event) => changeRate(event.target.value)} />
      </label>
      <label className="rate-line">
        <span>{strings.rateTotal(currencySymbol(to))}</span>
        <input className="rate-input" inputMode="decimal" autoComplete="off" aria-label={strings.rateTotal(currencySymbol(to))} disabled={amount === null} value={totalText} onChange={(event) => changeTotal(event.target.value)} />
      </label>
      {custom && <button type="button" className="link-button" onClick={() => { onReset(); onClose() }}>{strings.rateReset}</button>}
      <div className="rate-remember">
        <span>{strings.rateRemember(currencySymbol(from))}</span>
        <Switch checked={remember} label={strings.rateRemember(currencySymbol(from))} onChange={setRemember} />
      </div>
      <Button disabled={typedRate === null} onClick={done}>{strings.done}</Button>
    </Sheet>
  )
}
