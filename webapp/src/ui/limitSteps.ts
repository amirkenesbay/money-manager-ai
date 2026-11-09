import type { Currency } from '../api/types'

const MANTISSAS = [1, 1.5, 2, 2.5, 3, 4, 5, 6, 7.5]
const DECIMAL_BASE = 10

const LIMIT_RANGE: Record<Currency, { min: number; max: number }> = {
  KZT: { min: 5_000, max: 1_000_000 },
  RUB: { min: 500, max: 200_000 },
  KGS: { min: 500, max: 200_000 },
  USD: { min: 10, max: 2_000 },
  EUR: { min: 10, max: 2_000 },
}

export const NO_LIMIT_STEP = 0

export function limitSteps(currency: Currency): number[] {
  const { min, max } = LIMIT_RANGE[currency]
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
