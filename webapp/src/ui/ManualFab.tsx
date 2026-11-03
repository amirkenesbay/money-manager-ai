import { type PointerEvent, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Hint } from '../onboarding/Hint'
import { strings } from '../strings'
import { clampFabPoint, type FabArea, type FabPoint, fabPoint, loadFabPosition, saveFabPosition, snapFabPosition } from './fabPosition'
import { PlusIcon } from './icons'

const TAP_DISTANCE = 6
const FALLBACK_SIZE = 56
const SAFE_TOP = 72
const DOCK_CLEARANCE = 120
const COLUMN_WIDTH = 480
const SCROLL_HIDE_OFFSET = 24

interface DragStart {
  x: number
  y: number
  offsetX: number
  offsetY: number
  moved: boolean
}

export function ManualFab({ onOpen }: { onOpen: () => void }) {
  const button = useRef<HTMLButtonElement>(null)
  const start = useRef<DragStart | null>(null)
  const [position, setPosition] = useState(loadFabPosition)
  const [drag, setDrag] = useState<FabPoint | null>(null)
  const [size, setSize] = useState(FALLBACK_SIZE)
  const area = useFabArea()
  const hidden = useHiddenOnScrollDown() && drag === null
  const point = drag ?? fabPoint(position, area, size)

  useLayoutEffect(() => {
    if (button.current?.offsetWidth) setSize(button.current.offsetWidth)
  }, [])

  const press = (event: PointerEvent<HTMLButtonElement>) => {
    const box = event.currentTarget.getBoundingClientRect()
    start.current = { x: event.clientX, y: event.clientY, offsetX: event.clientX - box.left, offsetY: event.clientY - box.top, moved: false }
    event.currentTarget.setPointerCapture?.(event.pointerId)
  }

  const move = (event: PointerEvent<HTMLButtonElement>) => {
    const current = start.current
    if (!current) return
    if (!current.moved && Math.hypot(event.clientX - current.x, event.clientY - current.y) < TAP_DISTANCE) return
    current.moved = true
    setDrag(clampFabPoint({ x: event.clientX - current.offsetX, y: event.clientY - current.offsetY }, area, size))
  }

  const release = () => {
    const current = start.current
    start.current = null
    if (!current) return
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
