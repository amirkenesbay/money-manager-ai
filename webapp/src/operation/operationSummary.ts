import type { Currency, Operation } from '../api/types'
import { formatDayLabel, formatSignedMoney } from '../format'

export function operationSummary(operation: Operation, currency: Currency, language: string): string {
  return [
    formatSignedMoney(operation.type, operation.amount, currency, language),
    operation.categoryName,
    formatDayLabel(operation.operationDate, new Date(), language),
  ].join(' · ')
}
