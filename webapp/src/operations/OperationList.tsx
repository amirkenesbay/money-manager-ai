import type { Currency, Operation } from '../api/types'
import { formatShortDate, formatSignedMoney } from '../format'

export function OperationList({ operations, currency, language }: {
  operations: Operation[]
  currency: Currency
  language: string
}) {
  return (
    <ul className="operations">
      {operations.map((operation) => (
        <li key={operation.id}>
          <span className="operation-icon">{operation.categoryIcon}</span>
          <span className="operation-main">
            <span>{operation.categoryName}</span>
            <span className="hint">{[formatShortDate(operation.operationDate, language), operation.description].filter(Boolean).join(' · ')}</span>
          </span>
          <span className={operation.type === 'INCOME' ? 'positive' : 'negative'}>
            {formatSignedMoney(operation.type, operation.amount, currency, language)}
          </span>
        </li>
      ))}
    </ul>
  )
}
