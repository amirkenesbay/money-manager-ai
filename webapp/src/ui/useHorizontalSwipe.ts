import { type PointerEvent, useRef } from 'react'
import { selectionTick } from '../telegram/haptics'

const SWIPE_PX = 60
const VERTICAL_LIMIT = 0.6

export function useHorizontalSwipe(onSwipeLeft: () => void, onSwipeRight: () => void) {
  const start = useRef<{ x: number; y: number } | null>(null)
  return {
    onPointerDown: (event: PointerEvent<HTMLElement>) => {
      start.current = { x: event.clientX, y: event.clientY }
    },
    onPointerUp: (event: PointerEvent<HTMLElement>) => {
      const from = start.current
      start.current = null
      if (!from) return
      const dx = event.clientX - from.x
      const dy = event.clientY - from.y
      if (Math.abs(dx) < SWIPE_PX || Math.abs(dy) > Math.abs(dx) * VERTICAL_LIMIT) return
      selectionTick()
      if (dx < 0) onSwipeLeft()
      else onSwipeRight()
    },
    onPointerCancel: () => {
      start.current = null
    },
  }
}
