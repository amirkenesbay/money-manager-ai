import type { Group, Operation } from '../api/types'
import { deleteOperation } from '../operations/operationsApi'
import { strings } from '../strings'
import { ConfirmSheet } from '../ui/ConfirmSheet'
import { operationSummary } from './operationSummary'

export function DeleteSheet({ group, language, operation, onDeleted, onClose }: {
  group: Group
  language: string
  operation: Operation
  onDeleted: (operation: Operation) => void
  onClose: () => void
}) {
  return (
    <ConfirmSheet
      title={strings.deleteQuestion}
      confirmLabel={strings.delete}
      onConfirm={() => deleteOperation(group.id, operation.id)}
      onDone={() => onDeleted(operation)}
      onClose={onClose}
    >
      <p className="hint">{strings.deleteWarning(operationSummary(operation, group.currency, language))}</p>
    </ConfirmSheet>
  )
}
