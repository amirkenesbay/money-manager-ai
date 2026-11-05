import { type CSSProperties, type PointerEvent, useRef, useState } from 'react'

const START_SLOP_PX = 6
const CLOSE_SHARE = 0.25
const CLOSE_DELAY_MS = 180

export function useDragDismiss(onDismiss: () => void) {
  const start = useRef<number | null>(null)
  const height = useRef(0)
  const [offset, setOffset] = useState(0)
  const [dragging, setDragging] = useState(false)

  const style: CSSProperties | undefined = offset > 0 ? { transform: `translateY(${offset}px)`, transition: dragging ? 'none' : undefined } : undefined

  return {
    style,
    grip: {
      onPointerDown: (event: PointerEvent<HTMLElement>) => {
        start.current = event.clientY
        height.current = event.currentTarget.parentElement?.offsetHeight ?? 0
        event.currentTarget.setPointerCapture?.(event.pointerId)
      },
      onPointerMove: (event: PointerEvent<HTMLElement>) => {
        if (start.current === null) return
        const next = Math.max(0, event.clientY - start.current)
        if (next <= START_SLOP_PX) return
        setDragging(true)
        setOffset(next)
      },
      onPointerUp: () => {
        if (start.current === null) return
        start.current = null
        setDragging(false)
        if (offset > height.current * CLOSE_SHARE) {
          setOffset(height.current)
          setTimeout(onDismiss, CLOSE_DELAY_MS)
          return
        }
        setOffset(0)
      },
      onPointerCancel: () => {
        start.current = null
        setDragging(false)
        setOffset(0)
      },
    },
  }
}
