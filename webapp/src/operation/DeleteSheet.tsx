import { useState } from 'react'
import { serverMessage } from '../api/client'
import type { Group, Operation } from '../api/types'
import { deleteOperation } from '../operations/operationsApi'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { operationSummary } from './operationSummary'

export function DeleteSheet({ group, language, operation, onDeleted, onClose }: {
  group: Group
  language: string
  operation: Operation
  onDeleted: (operation: Operation) => void
  onClose: () => void
}) {
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const confirm = async () => {
    setDeleting(true)
    setError(null)
    try {
      await deleteOperation(group.id, operation.id)
      notifyOutcome('success')
      onDeleted(operation)
    } catch (failure) {
      notifyOutcome('error')
      setError(serverMessage(failure) ?? strings.deleteFailed)
      setDeleting(false)
    }
  }

  return (
    <Sheet label={strings.deleteQuestion} onClose={onClose}>
      <h2 className="sheet-title">{strings.deleteQuestion}</h2>
      <p className="hint">{strings.deleteWarning(operationSummary(operation, group.currency, language))}</p>
      {error && <p className="error-text" role="alert">{error}</p>}
      <Button className="button-destructive" disabled={deleting} onClick={confirm}>{strings.delete}</Button>
      <Button variant="secondary" onClick={onClose}>{strings.keep}</Button>
    </Sheet>
  )
}
