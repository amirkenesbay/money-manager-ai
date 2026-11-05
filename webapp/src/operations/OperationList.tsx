import type { Currency, Operation } from '../api/types'
import { formatSignedMoney } from '../format'
import { TaggedText } from './TaggedText'
import { ImpactNote } from '../debts/ImpactNote'
import { strings } from '../strings'
import { SwipeRow } from '../ui/SwipeRow'
import { OPERATION_CARD, shareElement } from '../ui/sharedElement'
import type { OperationActions } from './operationActions'

export function OperationList({ operations, currency, language, userId, actions, note, onSelect }: {
  operations: Operation[]
  currency: Currency
  language: string
  userId?: number
  actions?: OperationActions
  note: (operation: Operation) => string
  onSelect: (operation: Operation) => void
}) {
  return (
    <ul className="rows">
      {operations.map((operation) => {
        const row = (
          <button
            type="button"
            className="row row-button pressable"
            onClick={(event) => {
              shareElement(event.currentTarget, OPERATION_CARD)
              onSelect(operation)
            }}
          >
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
        )
        return (
          <li key={operation.id}>
            {actions
              ? (
                <SwipeRow
                  leftLabel={strings.delete}
                  rightLabel={strings.edit}
                  onSwipeLeft={() => actions.remove(operation)}
                  onSwipeRight={() => actions.edit(operation)}
                  onLongPress={() => actions.menu(operation)}
                >
                  {row}
                </SwipeRow>
              )
              : row}
          </li>
        )
      })}
    </ul>
  )
}
