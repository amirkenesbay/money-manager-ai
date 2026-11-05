import { type KeyboardEvent, useRef, useState } from 'react'
import { holdImpact, notifyOutcome } from '../telegram/haptics'
import { Button } from './Button'

const HOLD_MS = 1200

export function HoldButton({ label, disabled = false, onComplete }: { label: string; disabled?: boolean; onComplete: () => void }) {
  const frame = useRef(0)
  const [progress, setProgress] = useState(0)

  const stop = () => {
    cancelAnimationFrame(frame.current)
    setProgress(0)
  }

  const start = () => {
    holdImpact()
    const started = performance.now()
    const tick = (now: number) => {
      const next = Math.min(1, (now - started) / HOLD_MS)
      setProgress(next)
      if (next < 1) {
        frame.current = requestAnimationFrame(tick)
        return
      }
      notifyOutcome('success')
      setProgress(0)
      onComplete()
    }
    frame.current = requestAnimationFrame(tick)
  }

  const keyboard = (event: KeyboardEvent) => {
    if (event.key !== 'Enter' && event.key !== ' ') return
    event.preventDefault()
    onComplete()
  }

  return (
    <Button
      className="hold-button"
      disabled={disabled}
      onPointerDown={start}
      onPointerUp={stop}
      onPointerLeave={stop}
      onPointerCancel={stop}
      onKeyDown={keyboard}
      onContextMenu={(event) => event.preventDefault()}
    >
      <span className="hold-progress" style={{ transform: `scaleX(${progress})` }} aria-hidden="true" />
      <span className="hold-label">{label}</span>
    </Button>
  )
}
