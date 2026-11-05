import { useEffect, useState } from 'react'
import { prefersLessMotion } from './motion'

const STAGE_MS = 1800

export function ThinkingDots({ stages }: { stages: string[] }) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (stages.length < 2 || prefersLessMotion()) return undefined
    const timer = setInterval(() => setIndex((value) => Math.min(value + 1, stages.length - 1)), STAGE_MS)
    return () => clearInterval(timer)
  }, [stages.length])

  return (
    <div className="thinking">
      <span className="thinking-orb" aria-hidden="true"><i /><i /><i /></span>
      <p key={index} className="thinking-stage" aria-live="polite">{stages[index]}</p>
    </div>
  )
}
