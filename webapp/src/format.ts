import type { CategoryType, Currency } from './api/types'
import { strings } from './strings'

const FRACTION_DIGITS = 2
const POSITIVE_SIGN = '+'
const NEGATIVE_SIGN = '−'
const NO_BREAK_SPACE = '\u00A0'

const CURRENCY_SYMBOLS: Record<Currency, string> = {
  KZT: '₸',
  USD: '$',
  RUB: '₽',
  EUR: '€',
  KGS: 'с',
}

export function formatMoney(amount: string, currency: Currency, language: string): string {
  const value = Math.abs(Number(amount))
  const fractionDigits = Number.isInteger(value) ? 0 : FRACTION_DIGITS
  const number = new Intl.NumberFormat(language, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value)
  const formatted = `${number}${NO_BREAK_SPACE}${CURRENCY_SYMBOLS[currency]}`
  return Number(amount) < 0 ? `${NEGATIVE_SIGN}${formatted}` : formatted
}

export function formatSignedMoney(type: CategoryType, amount: string, currency: Currency, language: string): string {
  const formatted = formatMoney(amount, currency, language)
  if (Number(amount) === 0) return formatted
  const sign = type === 'INCOME' ? POSITIVE_SIGN : NEGATIVE_SIGN
  return `${sign}${formatted}`
}

export function formatDayLabel(isoDate: string, today: Date, language: string): string {
  if (isoDate === toIsoDate(today)) return strings.today
  if (isoDate === toIsoDate(new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1))) return strings.yesterday
  return new Intl.DateTimeFormat(language, { day: 'numeric', month: 'long' }).format(parseLocalDate(isoDate))
}

export function toIsoDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function parseLocalDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day)
}
