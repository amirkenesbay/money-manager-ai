import type { Currency, Operation } from '../api/types'
import { strings } from '../strings'
import { Sheet } from '../ui/Sheet'
import { operationSummary } from './operationSummary'

export function OperationMenu({ operation, currency, language, onRepeat, onRecurring, onCopy, onDelete, onClose }: {
  operation: Operation
  currency: Currency
  language: string
  onRepeat: () => void
  onRecurring: () => void
  onCopy: () => void
  onDelete: () => void
  onClose: () => void
}) {
  const choose = (action: () => void) => () => {
    onClose()
    action()
  }
  return (
    <Sheet label={strings.operationTitle} onClose={onClose}>
      <h2 className="sheet-title">{operationSummary(operation, currency, language)}</h2>
      <div className="split-options">
        <button type="button" className="split-option" onClick={choose(onRepeat)}>{strings.operationRepeatToday}</button>
        <button type="button" className="split-option" onClick={choose(onRecurring)}>{strings.operationMakeRecurring}</button>
        <button type="button" className="split-option" onClick={choose(onCopy)}>{strings.operationCopy}</button>
        <button type="button" className="split-option negative" onClick={choose(onDelete)}>{strings.deleteOperation}</button>
      </div>
    </Sheet>
  )
}
