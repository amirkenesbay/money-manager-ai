import type { Currency } from '../api/types'
import { currencySymbol } from '../format'

export const currencyBadge = (currency: Currency): string => `${currencySymbol(currency)} ${currency}`
