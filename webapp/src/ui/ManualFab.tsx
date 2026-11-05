import { type CSSProperties, type PointerEvent, useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { Category } from '../api/types'
import { holdImpact } from '../telegram/haptics'
import { Hint } from '../onboarding/Hint'
import { strings } from '../strings'
import { clampFabPoint, type FabArea, type FabPoint, fabPoint, loadFabPosition, saveFabPosition, snapFabPosition, type FabSide } from './fabPosition'
import { PlusIcon } from './icons'

const TAP_DISTANCE = 6
const FALLBACK_SIZE = 56
const SAFE_TOP = 72
const DOCK_CLEARANCE = 120
const COLUMN_WIDTH = 480
const SCROLL_HIDE_OFFSET = 24
const HOLD_MS = 450
const FAN_RADIUS_REM = 5
const FAN_SPREAD_DEG = 90

interface DragStart {
  x: number
  y: number
  offsetX: number
  offsetY: number
  moved: boolean
  held: boolean
}

export function ManualFab({ quickCategories = [], onOpen }: { quickCategories?: Category[]; onOpen: (category?: Category) => void }) {
  const button = useRef<HTMLButtonElement>(null)
  const start = useRef<DragStart | null>(null)
  const [position, setPosition] = useState(loadFabPosition)
  const [drag, setDrag] = useState<FabPoint | null>(null)
  const [size, setSize] = useState(FALLBACK_SIZE)
  const [fanOpen, setFanOpen] = useState(false)
  const hold = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const area = useFabArea()
  const hidden = useHiddenOnScrollDown() && drag === null
  const point = drag ?? fabPoint(position, area, size)

  useLayoutEffect(() => {
    if (button.current?.offsetWidth) setSize(button.current.offsetWidth)
  }, [])

  const press = (event: PointerEvent<HTMLButtonElement>) => {
    const box = event.currentTarget.getBoundingClientRect()
    start.current = { x: event.clientX, y: event.clientY, offsetX: event.clientX - box.left, offsetY: event.clientY - box.top, moved: false, held: false }
    event.currentTarget.setPointerCapture?.(event.pointerId)
    if (quickCategories.length === 0) return
    hold.current = setTimeout(() => {
      const current = start.current
      if (!current || current.moved) return
      current.held = true
      holdImpact()
      setFanOpen(true)
    }, HOLD_MS)
  }

  const pick = (category: Category) => {
    setFanOpen(false)
    onOpen(category)
  }

  const move = (event: PointerEvent<HTMLButtonElement>) => {
    const current = start.current
    if (!current) return
    if (!current.moved && Math.hypot(event.clientX - current.x, event.clientY - current.y) < TAP_DISTANCE) return
    current.moved = true
    clearTimeout(hold.current)
    setDrag(clampFabPoint({ x: event.clientX - current.offsetX, y: event.clientY - current.offsetY }, area, size))
  }

  const release = () => {
    const current = start.current
    start.current = null
    clearTimeout(hold.current)
    if (!current || current.held) return
    if (!current.moved || drag === null) {
      setDrag(null)
      onOpen()
      return
    }
    const next = snapFabPosition(drag, area, size)
    saveFabPosition(next)
    setPosition(next)
    setDrag(null)
  }

  const classes = ['manual-fab', position.side, drag ? 'dragging' : '', hidden ? 'hidden' : ''].filter(Boolean).join(' ')
  return (
    <div className={classes} style={{ left: point.x, top: point.y }}>
      {fanOpen && <button type="button" className="fab-fan-scrim" aria-label={strings.cancel} onClick={() => setFanOpen(false)} />}
      {fanOpen && quickCategories.map((category, index) => (
        <button
          key={category.id}
          type="button"
          className="fab-fan-item pressable"
          aria-label={category.name}
          style={fanStyle(index, quickCategories.length, position.side)}
          onClick={() => pick(category)}
        >
          {category.icon}
        </button>
      ))}
      <div className="manual-fab-hint">
        <Hint hint="MANUAL_ENTRY" title={strings.hintManualTitle} body={strings.hintManualBody} placement="above" />
      </div>
      <button
        ref={button}
        type="button"
        className="manual-fab-button"
        aria-label={strings.manualEntryLabel}
        onPointerDown={press}
        onPointerMove={move}
        onPointerUp={release}
        onPointerCancel={() => { start.current = null; setDrag(null) }}
      >
        <PlusIcon />
      </button>
    </div>
  )
}

function fanStyle(index: number, count: number, side: FabSide): CSSProperties {
  const step = count > 1 ? FAN_SPREAD_DEG / (count - 1) : 0
  const degrees = (side === 'left' ? 0 : FAN_SPREAD_DEG) + index * step
  const radians = (degrees * Math.PI) / 180
  return { '--fan-x': `${Math.cos(radians) * FAN_RADIUS_REM}rem`, '--fan-y': `${-Math.sin(radians) * FAN_RADIUS_REM}rem` } as CSSProperties
}

function useFabArea(): FabArea {
  const [area, setArea] = useState(measureArea)
  useEffect(() => {
    const update = () => setArea(measureArea())
    window.addEventListener('resize', update)
    return () => window.removeEventListener('resize', update)
  }, [])
  return area
}

function measureArea(): FabArea {
  const width = Math.min(window.innerWidth, COLUMN_WIDTH)
  return { left: (window.innerWidth - width) / 2, width, top: SAFE_TOP, bottom: window.innerHeight - DOCK_CLEARANCE }
}

function useHiddenOnScrollDown(): boolean {
  const [hidden, setHidden] = useState(false)
  useEffect(() => {
    let last = window.scrollY
    const update = () => {
      const now = window.scrollY
      setHidden(now > last && now > SCROLL_HIDE_OFFSET)
      last = now
    }
    window.addEventListener('scroll', update, { passive: true })
    return () => window.removeEventListener('scroll', update)
  }, [])
  return hidden
}
