import { type PointerEvent, useRef, useState } from 'react'
import { notifyOutcome } from '../telegram/haptics'
import { TrashIcon } from './icons'

const CONFIRM_RATIO = 0.85
const KEYBOARD_ACTIVATION = 0

export function SlideToConfirm({ label, disabled, onConfirm }: { label: string; disabled: boolean; onConfirm: () => void }) {
  const track = useRef<HTMLDivElement>(null)
  const thumb = useRef<HTMLButtonElement>(null)
  const grabbedAt = useRef<number | null>(null)
  const [offset, setOffset] = useState(0)
  const [dragging, setDragging] = useState(false)

  const maxOffset = () => (track.current?.clientWidth ?? 0) - (thumb.current?.offsetWidth ?? 0)

  const grab = (event: PointerEvent<HTMLButtonElement>) => {
    if (disabled) return
    event.currentTarget.setPointerCapture?.(event.pointerId)
    grabbedAt.current = event.clientX - offset
    setDragging(true)
  }

  const move = (event: PointerEvent<HTMLButtonElement>) => {
    if (grabbedAt.current === null) return
    setOffset(Math.min(maxOffset(), Math.max(0, event.clientX - grabbedAt.current)))
  }

  const release = () => {
    if (grabbedAt.current === null) return
    grabbedAt.current = null
    setDragging(false)
    const max = maxOffset()
    if (max > 0 && offset >= max * CONFIRM_RATIO) {
      setOffset(max)
      notifyOutcome('warning')
      onConfirm()
    } else {
      setOffset(0)
    }
  }

  return (
    <div ref={track} className={`slide-confirm ${dragging ? '' : 'settled'}`} aria-disabled={disabled}>
      <span className="slide-confirm-fill" style={{ width: `calc(${offset}px + var(--slide-thumb))` }} aria-hidden="true" />
      <span className="slide-confirm-label" aria-hidden="true">{label} »</span>
      <button
        ref={thumb}
        type="button"
        className="slide-confirm-thumb"
        aria-label={label}
        disabled={disabled}
        style={{ transform: `translateX(${offset}px)` }}
        onPointerDown={grab}
        onPointerMove={move}
        onPointerUp={release}
        onPointerCancel={release}
        onClick={(event) => event.detail === KEYBOARD_ACTIVATION && onConfirm()}
      >
        <TrashIcon />
      </button>
    </div>
  )
}
