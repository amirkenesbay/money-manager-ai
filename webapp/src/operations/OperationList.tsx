import type { Currency, Operation } from '../api/types'
import { formatSignedMoney } from '../format'

export function OperationList({ operations, currency, language, note }: {
  operations: Operation[]
  currency: Currency
  language: string
  note: (operation: Operation) => string
}) {
  return (
    <ul className="rows">
      {operations.map((operation) => (
        <li key={operation.id} className="row">
          <span className="icon-circle" aria-hidden="true">{operation.categoryIcon}</span>
          <span className="row-main">
            <span className="row-title">{operation.categoryName}</span>
            <span className="caption">{note(operation)}</span>
          </span>
          <span className={`amount ${operation.type === 'INCOME' ? 'income' : ''}`}>
            {formatSignedMoney(operation.type, operation.amount, currency, language)}
          </span>
        </li>
      ))}
    </ul>
  )
}
