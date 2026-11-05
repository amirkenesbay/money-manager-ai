import type { Currency, Operation } from '../api/types'
import { formatSignedMoney } from '../format'
import { TaggedText } from './TaggedText'
import { ImpactNote } from '../debts/ImpactNote'

export function OperationList({ operations, currency, language, userId, note, onSelect }: {
  operations: Operation[]
  currency: Currency
  language: string
  userId?: number
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
              <span className="caption"><TaggedText text={note(operation)} /></span>
            </span>
            <span>
              <span className={`amount ${operation.type === 'INCOME' ? 'income' : ''}`}>
                {formatSignedMoney(operation.type, operation.amount, currency, language)}
              </span>
              {userId !== undefined && <ImpactNote operation={operation} userId={userId} currency={currency} language={language} />}
            </span>
          </button>
        </li>
      ))}
    </ul>
  )
}
