import type { ReactNode } from 'react'
import { strings } from '../strings'
import { Button } from './Button'
import { Sheet } from './Sheet'
import { useSubmit } from './useSubmit'

export function ConfirmSheet({ title, children, confirmLabel, onConfirm, onDone, onClose }: {
  title: string
  children: ReactNode
  confirmLabel: string
  onConfirm: () => Promise<void>
  onDone: () => void
  onClose: () => void
}) {
  const submit = useSubmit()

  const confirm = async () => {
    const result = await submit.run(onConfirm, strings.deleteFailed)
    if (result.ok) onDone()
  }

  return (
    <Sheet label={title} onClose={onClose}>
      <h2 className="sheet-title">{title}</h2>
      {children}
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      <Button className="button-destructive" disabled={submit.pending} onClick={confirm}>{confirmLabel}</Button>
      <Button variant="secondary" onClick={onClose}>{strings.keep}</Button>
    </Sheet>
  )
}
