import type { CategoryType, Currency } from './api/types'
import { strings } from './strings'

const FRACTION_DIGITS = 2
const POSITIVE_SIGN = '+'
const NEGATIVE_SIGN = '−'
const NO_BREAK_SPACE = '\u00A0'
const ISO_MONTH_LENGTH = 7

const CURRENCY_SYMBOLS: Record<Currency, string> = {
  KZT: '₸',
  USD: '$',
  RUB: '₽',
  EUR: '€',
  KGS: 'с',
}

export const currencySymbol = (currency: Currency): string => CURRENCY_SYMBOLS[currency]

export function formatMoney(amount: string, currency: Currency, language: string): string {
  const value = Math.abs(Number(amount))
  const fractionDigits = Number.isInteger(value) ? 0 : FRACTION_DIGITS
  const number = new Intl.NumberFormat(language, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value)
  const formatted = `${number}${NO_BREAK_SPACE}${currencySymbol(currency)}`
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

export function parseLocalDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function toIsoMonth(date: Date): string {
  return toIsoDate(date).slice(0, ISO_MONTH_LENGTH)
}

export function shiftMonth(isoMonth: string, delta: number): string {
  const [year, month] = isoMonth.split('-').map(Number)
  return toIsoMonth(new Date(year, month - 1 + delta, 1))
}

export function formatMonthTitle(isoMonth: string, language: string): string {
  const parts = new Intl.DateTimeFormat(language, { month: 'long', year: 'numeric' }).formatToParts(monthStart(isoMonth))
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((candidate) => candidate.type === type)?.value ?? ''
  return capitalize(`${part('month')} ${part('year')}`)
}

export function formatMonthName(isoMonth: string, language: string): string {
  return capitalize(new Intl.DateTimeFormat(language, { month: 'long' }).format(monthStart(isoMonth)))
}

export function formatCompactMoney(amount: number, language: string): string {
  return new Intl.NumberFormat(language, { notation: 'compact', maximumFractionDigits: 0 }).format(amount)
}

export function formatMonthShort(isoMonth: string, language: string): string {
  return new Intl.DateTimeFormat(language, { month: 'short' }).format(monthStart(isoMonth)).replace('.', '')
}

export function formatPeriod(from: string, to: string, language: string): string {
  const day = new Intl.DateTimeFormat(language, { day: 'numeric', month: 'short' })
  const short = (isoDate: string) => day.format(parseLocalDate(isoDate)).replace('.', '')
  return `${short(from)} — ${short(to)}`
}

function monthStart(isoMonth: string): Date {
  return parseLocalDate(`${isoMonth}-01`)
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

export function formatDate(isoDateTime: string, language: string): string {
  return new Intl.DateTimeFormat(language, { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(isoDateTime))
}

export const initialOf = (name: string): string => name.charAt(0)
