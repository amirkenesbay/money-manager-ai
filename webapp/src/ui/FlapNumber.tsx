import { useEffect, useState } from 'react'

const SETTLE_MS = 600
const remembered = new Map<string, string>()

export function FlapNumber({ text, memoryKey, className }: { text: string; memoryKey: string; className?: string }) {
  const [before, setBefore] = useState(() => remembered.get(memoryKey) ?? text)

  useEffect(() => {
    remembered.set(memoryKey, text)
    if (before === text) return undefined
    const timer = setTimeout(() => setBefore(text), SETTLE_MS)
    return () => clearTimeout(timer)
  }, [memoryKey, text, before])

  const shift = before.length - text.length
  return (
    <span className={className}>
      <span className="visually-hidden">{text}</span>
      {Array.from(text).map((char, index) => {
        const changed = before[index + shift] !== char
        return (
          <span key={`${index}-${char}-${changed}`} className={`flap-digit ${changed ? 'flip' : ''}`} aria-hidden="true">{char}</span>
        )
      })}
    </span>
  )
}
