import type { Currency, ForeignAmount } from '../api/types'
import { CURRENCIES } from '../currencies'
import { currencySymbol, formatMoney } from '../format'
import { normalizeNumber } from './amount'

const CENTS = 100
const RATE_DIGITS = 2
const RATE_INPUT_DIGITS = 4
const LAST_CURRENCY_KEY = 'mm.operation.currency.'
const REMEMBERED_RATE_KEY = 'mm.operation.rate.'
const RATE_PATTERN = /^\d+(\.\d+)?$/

export const APPROXIMATELY = '≈'

export interface RateChoice {
  value: number
  custom: boolean
}

export function convertAmount(amount: string, rate: number): string {
  return String(Math.round(Number(amount) * rate * CENTS) / CENTS)
}

export function parseRate(raw: string): number | null {
  const normalized = normalizeNumber(raw)
  const rate = Number(normalized)
  return RATE_PATTERN.test(normalized) && rate > 0 ? rate : null
}

export function rateText(rate: number, from: Currency, to: Currency, language: string): string {
  const format = new Intl.NumberFormat(language, { maximumFractionDigits: RATE_DIGITS })
  return rate >= 1
    ? `1 ${currencySymbol(from)} = ${format.format(rate)} ${currencySymbol(to)}`
    : `1 ${currencySymbol(to)} = ${format.format(1 / rate)} ${currencySymbol(from)}`
}

export function rateInputText(rate: number, language: string): string {
  return new Intl.NumberFormat(language, { maximumFractionDigits: RATE_INPUT_DIGITS, useGrouping: false }).format(rate)
}

export function originalText({ original }: { original?: ForeignAmount | null }, language: string): string | null {
  return original ? formatMoney(original.amount, original.currency, language) : null
}

export function lastCurrency(groupId: string): Currency | null {
  const stored = readStored(LAST_CURRENCY_KEY + groupId)
  return CURRENCIES.find((currency) => currency === stored) ?? null
}

export function rememberCurrency(groupId: string, currency: Currency): void {
  writeStored(LAST_CURRENCY_KEY + groupId, currency)
}

export function rememberedRate(groupId: string, currency: Currency): number | null {
  const rate = Number(readStored(REMEMBERED_RATE_KEY + groupId + currency))
  return rate > 0 ? rate : null
}

export function rememberRate(groupId: string, currency: Currency, rate: number | null): void {
  writeStored(REMEMBERED_RATE_KEY + groupId + currency, rate === null ? null : String(rate))
}

function readStored(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function writeStored(key: string, value: string | null): void {
  try {
    if (value === null) localStorage.removeItem(key)
    else localStorage.setItem(key, value)
  } catch {
    return
  }
}
