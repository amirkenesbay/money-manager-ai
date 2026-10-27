import type { Currency, Operation } from '../api/types'
import { formatSignedMoney } from '../format'

export function OperationList({ operations, currency, language, note, onSelect }: {
  operations: Operation[]
  currency: Currency
  language: string
  note: (operation: Operation) => string
  onSelect: (operation: Operation) => void
}) {
  return (
    <ul className="rows">
      {operations.map((operation) => (
        <li key={operation.id}>
          <button type="button" className="row row-button pressable" onClick={() => onSelect(operation)}>
            <span className="icon-circle" aria-hidden="true">{operation.categoryIcon}</span>
            <span className="row-main">
              <span className="row-title">{operation.categoryName}</span>
              <span className="caption">{note(operation)}</span>
            </span>
            <span className={`amount ${operation.type === 'INCOME' ? 'income' : ''}`}>
              {formatSignedMoney(operation.type, operation.amount, currency, language)}
            </span>
          </button>
        </li>
      ))}
    </ul>
  )
}
