import type { Currency } from '../api/types'
import { CURRENCY_CATALOG } from '../currencies'

const MANTISSAS = [1, 1.5, 2, 2.5, 3, 4, 5, 6, 7.5]
const DECIMAL_BASE = 10

const MIN_UNITS = 10
const MAX_UNITS = 2_000

export const NO_LIMIT_STEP = 0

export function limitSteps(currency: Currency): number[] {
  const unit = CURRENCY_CATALOG[currency].unit
  const min = unit * MIN_UNITS
  const max = unit * MAX_UNITS
  const steps = [NO_LIMIT_STEP]
  for (let power = Math.floor(Math.log10(min)); power <= Math.ceil(Math.log10(max)); power += 1) {
    for (const mantissa of MANTISSAS) {
      const value = mantissa * DECIMAL_BASE ** power
      if (value >= min && value <= max) steps.push(value)
    }
  }
  return steps
}

export function nearestStep(steps: number[], value: number | null): number {
  if (value === null || value <= NO_LIMIT_STEP) return 0
  return steps.reduce((best, step, index) => (Math.abs(step - value) < Math.abs(steps[best] - value) ? index : best), 0)
}
