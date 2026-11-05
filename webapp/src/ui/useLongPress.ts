import { type MouseEvent, useEffect, useRef } from 'react'

const LONG_PRESS_MS = 500

export function useLongPress(onPress: () => void, onLongPress: () => void) {
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const fired = useRef(false)
  const release = () => clearTimeout(timer.current)

  useEffect(() => release, [])

  return {
    onPointerDown: () => {
      fired.current = false
      release()
      timer.current = setTimeout(() => {
        fired.current = true
        onLongPress()
      }, LONG_PRESS_MS)
    },
    onPointerUp: release,
    onPointerLeave: release,
    onPointerCancel: release,
    onContextMenu: (event: MouseEvent) => event.preventDefault(),
    onClick: () => {
      if (fired.current) {
        fired.current = false
        return
      }
      onPress()
    },
  }
}
