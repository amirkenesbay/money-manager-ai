import type { CSSProperties } from 'react'
import type { Currency } from '../api/types'
import { formatMoney } from '../format'
import { strings } from '../strings'
import { selectionTick } from '../telegram/haptics'
import { FULL_PERCENT } from '../percent'
import { limitSteps, nearestStep, NO_LIMIT_STEP } from './limitSteps'

export function LimitSlider({ label, value, currency, language, onChange }: {
  label: string
  value: number | null
  currency: Currency
  language: string
  onChange: (value: string) => void
}) {
  const steps = limitSteps(currency)
  const index = nearestStep(steps, value)
  return (
    <input
      className="limit-slider"
      type="range"
      min={0}
      max={steps.length - 1}
      step={1}
      value={index}
      aria-label={label}
      aria-valuetext={steps[index] === NO_LIMIT_STEP ? strings.budgetNoLimit : formatMoney(String(steps[index]), currency, language)}
      style={{ '--fill': `${(index / (steps.length - 1)) * FULL_PERCENT}%` } as CSSProperties}
      onChange={(event) => {
        const next = steps[Number(event.target.value)]
        selectionTick()
        onChange(next === NO_LIMIT_STEP ? '' : String(next))
      }}
    />
  )
}
