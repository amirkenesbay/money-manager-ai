import { type CSSProperties, useEffect, useLayoutEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { strings } from '../strings'
import { selectionTick } from '../telegram/haptics'
import { Button } from '../ui/Button'
import { type TourTarget, tourTargetSelector } from './tourTarget'
import './tour.css'

export interface TourStep {
  target: TourTarget
  title: string
  body: string
}

const HOLE_PADDING = 6
const READY_TIMEOUT_MS = 3000
const LOADING_SELECTOR = '[aria-busy="true"]'
const CARD_GAP = 14
const CARD_MIN_SPACE = 200

type Box = { top: number; left: number; width: number; height: number }

const targetOf = (step: TourStep): HTMLElement | null => document.querySelector(tourTargetSelector(step.target))

function boxOf(element: HTMLElement): Box {
  const rect = element.getBoundingClientRect()
  const top = Math.max(rect.top - HOLE_PADDING, 0)
  const bottom = Math.min(rect.bottom + HOLE_PADDING, window.innerHeight)
  return { top, left: rect.left - HOLE_PADDING, width: rect.width + HOLE_PADDING * 2, height: Math.max(bottom - top, 0) }
}

const sameBox = (left: Box | null, right: Box): boolean =>
  left !== null && left.top === right.top && left.left === right.left && left.width === right.width && left.height === right.height

function useTrackedBox(step: TourStep | undefined): Box | null {
  const [box, setBox] = useState<Box | null>(null)
  useLayoutEffect(() => {
    if (!step) return undefined
    targetOf(step)?.scrollIntoView?.({ block: 'center' })
    let frame = 0
    const follow = () => {
      const element = targetOf(step)
      if (element) {
        const next = boxOf(element)
        setBox((current) => (sameBox(current, next) ? current : next))
      }
      frame = requestAnimationFrame(follow)
    }
    follow()
    return () => cancelAnimationFrame(frame)
  }, [step])
  return box
}

function useAvailableSteps(steps: TourStep[]): TourStep[] | null {
  const [available, setAvailable] = useState<TourStep[] | null>(null)
  useEffect(() => {
    const startedAt = performance.now()
    let frame = 0
    const settle = () => {
      const settled = targetOf(steps[0]) !== null && document.querySelector(LOADING_SELECTOR) === null
      if (settled || performance.now() - startedAt > READY_TIMEOUT_MS) {
        setAvailable(steps.filter((step) => targetOf(step) !== null))
        return
      }
      frame = requestAnimationFrame(settle)
    }
    frame = requestAnimationFrame(settle)
    return () => cancelAnimationFrame(frame)
  }, [steps])
  return available
}

function cardPlacement(box: Box): CSSProperties {
  const spaceAbove = box.top
  const spaceBelow = window.innerHeight - (box.top + box.height)
  if (Math.max(spaceAbove, spaceBelow) < CARD_MIN_SPACE) return { bottom: CARD_GAP }
  return spaceBelow >= spaceAbove ? { top: box.top + box.height + CARD_GAP } : { bottom: window.innerHeight - box.top + CARD_GAP }
}

export function SpotlightTour({ steps, finishLabel, laterLabel, onFinish }: {
  steps: TourStep[]
  finishLabel: string
  laterLabel?: string
  onFinish: (completed: boolean) => void
}) {
  const present = useAvailableSteps(steps)
  const [index, setIndex] = useState(0)
  const step = present?.[index]
  const box = useTrackedBox(step)
  const last = present !== null && index === present.length - 1

  useEffect(() => {
    if (present?.length === 0) onFinish(false)
  }, [present, onFinish])

  useEffect(() => {
    const skipOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onFinish(false)
    }
    document.addEventListener('keydown', skipOnEscape)
    return () => document.removeEventListener('keydown', skipOnEscape)
  }, [onFinish])

  if (!present || !step || !box) return null

  const next = () => {
    selectionTick()
    if (last) onFinish(true)
    else setIndex(index + 1)
  }

  return createPortal(
    <div className="tour" role="dialog" aria-modal="true" aria-label={step.title}>
      <span className="tour-hole" style={box} aria-hidden="true" />
      <section key={step.target} className="tour-card" style={cardPlacement(box)}>
        {present.length > 1 && <span className="caption">{strings.welcomeStep(index + 1, present.length)}</span>}
        <strong className="tour-title">{step.title}</strong>
        <p className="tour-body">{step.body}</p>
        <span className="tour-actions">
          {(!last || laterLabel) && <button type="button" className="link-button" onClick={() => onFinish(false)}>{last ? laterLabel : strings.welcomeSkip}</button>}
          <Button size="small" onClick={next}>{last ? finishLabel : strings.welcomeNext}</Button>
        </span>
      </section>
    </div>,
    document.body,
  )
}
