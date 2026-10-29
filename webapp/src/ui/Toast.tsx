import { useEffect } from 'react'
import { tapImpact } from '../telegram/haptics'
import { Moment } from '../three/Moment'

export interface ToastMessage {
  id: number
  title: string
  detail?: string
  celebrate?: boolean
  action?: { label: string; onClick: () => void }
}

const TOAST_DURATION_MS = 5000
const COIN_INPUT = { onLanded: tapImpact }

export function Toast({ toast, onDone }: { toast: ToastMessage; onDone: () => void }) {
  useEffect(() => {
    const timer = setTimeout(onDone, TOAST_DURATION_MS)
    return () => clearTimeout(timer)
  }, [toast.id, onDone])

  const check = <span className="icon-circle toast-check" aria-hidden="true">✓</span>
  return (
    <div key={toast.id} className="toast" role="status">
      {toast.celebrate ? <Moment kind="coin" input={COIN_INPUT} className="toast-coin" fallback={check} /> : check}
      <span className="toast-text">
        <span className="row-title">{toast.title}</span>
        {toast.detail && <span className="caption">{toast.detail}</span>}
      </span>
      {toast.action && (
        <button type="button" className="toast-action pressable" onClick={toast.action.onClick}>{toast.action.label}</button>
      )}
      <span className="toast-progress" style={{ animationDuration: `${TOAST_DURATION_MS}ms` }} aria-hidden="true" />
    </div>
  )
}
