export type FabSide = 'left' | 'right'

export interface FabPosition {
  side: FabSide
  ratio: number
}

export interface FabArea {
  left: number
  width: number
  top: number
  bottom: number
}

export interface FabPoint {
  x: number
  y: number
}

const STORAGE_KEY = 'manual-fab-position'
const EDGE_MARGIN = 16

export const DEFAULT_FAB_POSITION: FabPosition = { side: 'right', ratio: 1 }

const clamp = (value: number, min: number, max: number): number => Math.min(max, Math.max(min, value))

export function fabPoint(position: FabPosition, area: FabArea, size: number): FabPoint {
  const travel = Math.max(0, area.bottom - area.top - size)
  return {
    x: area.left + (position.side === 'left' ? EDGE_MARGIN : area.width - size - EDGE_MARGIN),
    y: area.top + position.ratio * travel,
  }
}

export function clampFabPoint(point: FabPoint, area: FabArea, size: number): FabPoint {
  return {
    x: clamp(point.x, area.left, area.left + area.width - size),
    y: clamp(point.y, area.top, Math.max(area.top, area.bottom - size)),
  }
}

export function snapFabPosition(point: FabPoint, area: FabArea, size: number): FabPosition {
  const travel = Math.max(1, area.bottom - area.top - size)
  return {
    side: point.x + size / 2 < area.left + area.width / 2 ? 'left' : 'right',
    ratio: clamp((point.y - area.top) / travel, 0, 1),
  }
}

export function loadFabPosition(): FabPosition {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as FabPosition | null
    if (saved && (saved.side === 'left' || saved.side === 'right') && saved.ratio >= 0 && saved.ratio <= 1) return saved
  } catch {
    return DEFAULT_FAB_POSITION
  }
  return DEFAULT_FAB_POSITION
}

export function saveFabPosition(position: FabPosition): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(position))
  } catch {
    return
  }
}
