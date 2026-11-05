import { useEffect, useState } from 'react'
import { strings } from '../strings'
import { prefersLessMotion } from '../ui/motion'
import type { HintItem } from './hintItems'

const ROTATE_MS = 3000
const FADE_MS = 350

export function RotatingHint({ items, onAsk }: { items: HintItem[]; onAsk: (question: string) => void }) {
  const [index, setIndex] = useState(0)
  const [fading, setFading] = useState(false)

  useEffect(() => {
    if (items.length < 2) return undefined
    const next = () => setIndex((value) => (value + 1) % items.length)
    let fadeTimer: ReturnType<typeof setTimeout> | undefined
    const timer = setInterval(() => {
      if (prefersLessMotion()) {
        next()
        return
      }
      setFading(true)
      fadeTimer = setTimeout(() => {
        next()
        setFading(false)
      }, FADE_MS)
    }, ROTATE_MS)
    return () => {
      clearInterval(timer)
      clearTimeout(fadeTimer)
    }
  }, [items.length])

  const item = items[index % items.length]
  if (!item) return null
  const textClass = `ask-hint-text ${fading ? 'fading' : ''}`
  return (
    <div className="ask-hint">
      <span className="ask-hint-label">{item.kind === 'say' ? strings.hintSay : strings.hintAsk}</span>
      {item.kind === 'ask'
        ? <button type="button" className={`${textClass} question pressable`} onClick={() => onAsk(item.text)}>{strings.quoted(item.text)}</button>
        : <span className={textClass}>{item.text}</span>}
      {items.length > 1 && (
        <span className="ask-hint-dots" aria-hidden="true">
          {items.map((candidate, position) => <i key={candidate.text} className={position === index % items.length ? 'on' : ''} />)}
        </span>
      )}
    </div>
  )
}
