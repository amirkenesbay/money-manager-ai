import { useEffect, useRef, useState } from 'react'

const DURATION_MS = 500
const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'

export function useCountUp(target: number): number {
  const [value, setValue] = useState(target)
  const previous = useRef(target)

  useEffect(() => {
    const from = previous.current
    previous.current = target
    if (from === target || window.matchMedia?.(REDUCED_MOTION_QUERY).matches) {
      setValue(target)
      return undefined
    }
    const startedAt = performance.now()
    let frame = requestAnimationFrame(function step(now) {
      const progress = Math.min(1, (now - startedAt) / DURATION_MS)
      const eased = 1 - (1 - progress) ** 3
      setValue(from + (target - from) * eased)
      if (progress < 1) frame = requestAnimationFrame(step)
    })
    return () => cancelAnimationFrame(frame)
  }, [target])

  return value
}
