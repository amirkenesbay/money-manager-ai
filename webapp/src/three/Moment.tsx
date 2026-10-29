import { type ReactNode, useEffect, useRef, useState } from 'react'
import type { MomentInputs, MomentKind } from './moments'
import { supports3d } from './support'
import './moment.css'

type Engine = typeof import('./engine')

let engine: Promise<Engine> | null = null

const loadEngine = (): Promise<Engine> => (engine ??= import('./engine'))

export function Moment<K extends MomentKind>({ kind, input, fallback, className, label, overlay }: {
  kind: K
  input: MomentInputs[K]
  fallback: ReactNode
  className: string
  label?: string
  overlay?: ReactNode
}) {
  const [enabled] = useState(supports3d)
  const [ready, setReady] = useState(false)
  const canvas = useRef<HTMLCanvasElement>(null)
  const latest = useRef(input)

  useEffect(() => {
    latest.current = input
  })

  useEffect(() => {
    const target = canvas.current
    if (!enabled || !target) return undefined
    let detach: (() => void) | null = null
    let cancelled = false
    loadEngine()
      .then((loaded) => {
        if (cancelled) return
        detach = loaded.attach(target, kind, () => latest.current)
        setReady(true)
      })
      .catch(() => setReady(false))
    return () => {
      cancelled = true
      detach?.()
    }
  }, [enabled, kind])

  if (!enabled) return fallback
  return (
    <span className={`moment ${className}`}>
      {!ready && fallback}
      <canvas ref={canvas} className="moment-canvas" hidden={!ready} role={label ? 'img' : undefined} aria-label={label} aria-hidden={label ? undefined : true} />
      {ready && overlay}
    </span>
  )
}
