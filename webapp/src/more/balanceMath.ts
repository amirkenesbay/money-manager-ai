import type { Balance } from '../api/types'

const CENTS = 100

const toCents = (value: string): number => Math.round(Number(value) * CENTS)

export function initialForCurrent(balance: Balance, current: string): string {
  const movement = toCents(balance.total) - toCents(balance.initial)
  return ((toCents(current) - movement) / CENTS).toFixed(2)
}
