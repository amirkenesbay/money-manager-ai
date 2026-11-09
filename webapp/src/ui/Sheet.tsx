import { type ReactNode, useEffect, useRef } from 'react'
import { withMotion } from './motion'
import { useDragDismiss } from './useDragDismiss'

export function Sheet({ label, onClose, children }: { label: string; onClose: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDivElement>(null)
  const drag = useDragDismiss(onClose)

  const close = useRef(onClose)

  useEffect(() => {
    close.current = onClose
  })

  useEffect(() => {
    const previousFocus = document.activeElement as HTMLElement | null
    dialog.current?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') withMotion('dismiss', close.current)
    }
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('keydown', closeOnEscape)
      previousFocus?.focus()
    }
  }, [])

  return (
    <div className="sheet-scrim" onClick={(event) => event.target === event.currentTarget && withMotion('dismiss', onClose)}>
      <div ref={dialog} className="sheet" style={drag.style} role="dialog" aria-modal="true" aria-label={label} tabIndex={-1}>
        <span className="sheet-grip" aria-hidden="true" {...drag.grip}><span className="sheet-handle" /></span>
        {children}
      </div>
    </div>
  )
}
