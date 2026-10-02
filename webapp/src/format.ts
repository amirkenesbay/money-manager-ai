import type { CategoryType, Currency } from './api/types'

const FRACTION_DIGITS = 2
const POSITIVE_SIGN = '+'
const NEGATIVE_SIGN = '−'

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
  const formatted = `${number}${CURRENCY_SYMBOLS[currency]}`
  return Number(amount) < 0 ? `${NEGATIVE_SIGN}${formatted}` : formatted
}

export function formatSignedMoney(type: CategoryType, amount: string, currency: Currency, language: string): string {
  const formatted = formatMoney(amount, currency, language)
  if (Number(amount) === 0) return formatted
  const sign = type === 'INCOME' ? POSITIVE_SIGN : NEGATIVE_SIGN
  return `${sign}${formatted}`
}

export function formatShortDate(isoDate: string, language: string): string {
  return new Intl.DateTimeFormat(language, { day: 'numeric', month: 'short' }).format(parseLocalDate(isoDate))
}

export function formatMonthTitle(isoMonth: string, language: string): string {
  const title = new Intl.DateTimeFormat(language, { month: 'long', year: 'numeric' }).format(parseLocalDate(`${isoMonth}-01`))
  return title.charAt(0).toUpperCase() + title.slice(1)
}

function parseLocalDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day)
}
