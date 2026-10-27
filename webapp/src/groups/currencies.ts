import type { Currency } from '../api/types'
import { currencySymbol } from '../format'

export const CURRENCIES: Currency[] = ['KZT', 'USD', 'RUB', 'EUR', 'KGS']

export const currencyBadge = (currency: Currency): string => `${currencySymbol(currency)} ${currency}`
