import { describe, expect, it } from 'vitest'
import { launchMonth, launchScreen } from './launchScreen'

describe('launchScreen', () => {
  it('reads the screen the bot asked to open', () => {
    expect(launchScreen('?screen=add')).toBe('add')
    expect(launchScreen('?mock=empty&screen=categories')).toBe('categories')
    expect(launchScreen('?screen=operations')).toBe('operations')
    expect(launchScreen('?screen=groups')).toBe('groups')
  })

  it('opens home for a missing or unknown screen', () => {
    expect(launchScreen('')).toBeNull()
    expect(launchScreen('?screen=settings')).toBeNull()
  })

  it('reads the report month the bot asked to open', () => {
    expect(launchScreen('?screen=reports&month=2026-10')).toBe('reports')
    expect(launchMonth('?screen=reports&month=2026-10')).toBe('2026-10')
    expect(launchMonth('?screen=reports&month=2026-13')).toBeNull()
    expect(launchMonth('')).toBeNull()
  })
})
