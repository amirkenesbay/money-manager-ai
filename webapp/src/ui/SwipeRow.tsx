import { type MouseEvent, type PointerEvent, type ReactNode, useRef, useState } from 'react'
import { holdImpact, selectionTick } from '../telegram/haptics'

const ACTION_SHARE = 0.4
const SWIPE_SLOP_PX = 10
const HOLD_MS = 450

interface Gesture {
  x: number
  y: number
  width: number
  swiping: boolean
  crossed: boolean
}

export function SwipeRow({ children, leftLabel, rightLabel, onSwipeLeft, onSwipeRight, onLongPress }: {
  children: ReactNode
  leftLabel: string
  rightLabel: string
  onSwipeLeft: () => void
  onSwipeRight: () => void
  onLongPress: () => void
}) {
  const gesture = useRef<Gesture | null>(null)
  const hold = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const handled = useRef(false)
  const [offset, setOffset] = useState(0)
  const [dragging, setDragging] = useState(false)

  const reset = () => {
    clearTimeout(hold.current)
    gesture.current = null
    setDragging(false)
    setOffset(0)
  }

  const down = (event: PointerEvent<HTMLDivElement>) => {
    handled.current = false
    gesture.current = { x: event.clientX, y: event.clientY, width: event.currentTarget.offsetWidth, swiping: false, crossed: false }
    hold.current = setTimeout(() => {
      handled.current = true
      gesture.current = null
      holdImpact()
      onLongPress()
    }, HOLD_MS)
  }

  const move = (event: PointerEvent<HTMLDivElement>) => {
    const current = gesture.current
    if (!current) return
    const dx = event.clientX - current.x
    const dy = event.clientY - current.y
    if (!current.swiping) {
      if (Math.abs(dy) > SWIPE_SLOP_PX) return reset()
      if (Math.abs(dx) <= SWIPE_SLOP_PX) return
      clearTimeout(hold.current)
      current.swiping = true
      event.currentTarget.setPointerCapture?.(event.pointerId)
      setDragging(true)
    }
    const crossed = Math.abs(dx) > current.width * ACTION_SHARE
    if (crossed !== current.crossed) selectionTick()
    current.crossed = crossed
    setOffset(dx)
  }

  const up = () => {
    const current = gesture.current
    clearTimeout(hold.current)
    if (current?.swiping) {
      handled.current = true
      if (current.crossed) (offset < 0 ? onSwipeLeft : onSwipeRight)()
    }
    reset()
  }

  const swallowClick = (event: MouseEvent) => {
    if (!handled.current) return
    event.preventDefault()
    event.stopPropagation()
    handled.current = false
  }

  return (
    <div className={`swipe-row ${offset < 0 ? 'to-left' : offset > 0 ? 'to-right' : ''}`}>
      <span className="swipe-under" aria-hidden="true">
        <span className="swipe-right">{rightLabel}</span>
        <span className="swipe-left">{leftLabel}</span>
      </span>
      <div
        className={`swipe-face ${dragging ? 'dragging' : ''}`}
        style={offset ? { transform: `translateX(${offset}px)` } : undefined}
        onPointerDown={down}
        onPointerMove={move}
        onPointerUp={up}
        onPointerCancel={reset}
        onPointerLeave={() => !gesture.current?.swiping && clearTimeout(hold.current)}
        onClickCapture={swallowClick}
        onContextMenu={(event) => event.preventDefault()}
      >
        {children}
      </div>
    </div>
  )
}
