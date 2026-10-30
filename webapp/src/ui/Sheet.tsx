import { type ReactNode, useEffect, useRef } from 'react'
import { withMotion } from './motion'

export function Sheet({ label, onClose, children }: { label: string; onClose: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const previousFocus = document.activeElement as HTMLElement | null
    dialog.current?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') withMotion('dismiss', onClose)
    }
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('keydown', closeOnEscape)
      previousFocus?.focus()
    }
  }, [onClose])

  return (
    <div className="sheet-scrim" onClick={(event) => event.target === event.currentTarget && withMotion('dismiss', onClose)}>
      <div ref={dialog} className="sheet" role="dialog" aria-modal="true" aria-label={label} tabIndex={-1}>
        <span className="sheet-handle" aria-hidden="true" />
        {children}
      </div>
    </div>
  )
}
