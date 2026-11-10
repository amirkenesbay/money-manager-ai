import type { Currency } from '../api/types'
import { CURRENCIES } from '../currencies'

const STORAGE_KEY = 'mm.converter.currencies'
const DEFAULT_FOREIGN: Currency[] = ['USD', 'EUR', 'RUB']
const MAX_CURRENCIES = 6

export function savedCurrencies(groupCurrency: Currency): Currency[] {
  const stored = readStored()
  return withGroupCurrency(stored.length > 0 ? stored : DEFAULT_FOREIGN, groupCurrency)
}

export function saveCurrencies(currencies: Currency[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(currencies.slice(0, MAX_CURRENCIES)))
  } catch {
    return
  }
}

export function withGroupCurrency(currencies: Currency[], groupCurrency: Currency): Currency[] {
  return [...new Set([groupCurrency, ...currencies])].slice(0, MAX_CURRENCIES)
}

function readStored(): Currency[] {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    return Array.isArray(parsed) ? parsed.filter((item): item is Currency => CURRENCIES.includes(item)) : []
  } catch {
    return []
  }
}
