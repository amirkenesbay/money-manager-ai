import { afterEach, describe, expect, it } from 'vitest'
import { clampFabPoint, DEFAULT_FAB_POSITION, fabPoint, loadFabPosition, saveFabPosition, snapFabPosition } from './fabPosition'

const SIZE = 56
const area = { left: 0, width: 390, top: 72, bottom: 660 }

describe('manual button position', () => {
  afterEach(() => localStorage.clear())

  it('sits at the bottom right by default', () => {
    expect(fabPoint(DEFAULT_FAB_POSITION, area, SIZE)).toEqual({ x: 390 - SIZE - 16, y: 660 - SIZE })
  })

  it('sticks to the nearer edge at the height where it was dropped', () => {
    expect(snapFabPosition({ x: 40, y: 72 }, area, SIZE)).toEqual({ side: 'left', ratio: 0 })
    expect(snapFabPosition({ x: 300, y: 660 - SIZE }, area, SIZE)).toEqual({ side: 'right', ratio: 1 })
  })

  it('cannot be dragged over the header, the tab bar or out of the column', () => {
    const column = { ...area, left: 100 }
    expect(clampFabPoint({ x: -50, y: 0 }, column, SIZE)).toEqual({ x: 100, y: 72 })
    expect(clampFabPoint({ x: 900, y: 900 }, column, SIZE)).toEqual({ x: 100 + 390 - SIZE, y: 660 - SIZE })
  })

  it('remembers where it was left and ignores broken data', () => {
    saveFabPosition({ side: 'left', ratio: 0.4 })
    expect(loadFabPosition()).toEqual({ side: 'left', ratio: 0.4 })

    localStorage.setItem('manual-fab-position', '{"side":"top","ratio":7}')
    expect(loadFabPosition()).toEqual(DEFAULT_FAB_POSITION)
  })
})
