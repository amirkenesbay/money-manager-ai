import { describe, expect, it } from 'vitest'
import { launchCategoryId, launchMonth, launchScreen, launchSource } from './launchScreen'

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

describe('launchCategoryId', () => {
  it('opens the form with the category from a reminder button', () => {
    expect(launchCategoryId('?screen=add&category=6650a1b2c3d4e5f6a7b8c9d0')).toBe('6650a1b2c3d4e5f6a7b8c9d0')
  })

  it('ignores anything that is not a category id', () => {
    expect(launchCategoryId('?category=../../me')).toBeNull()
  })
})

describe('launchSource', () => {
  it('reads which message opened the app', () => {
    expect(launchSource('?screen=add&src=spending_reminder')).toBe('spending_reminder')
    expect(launchSource('?screen=add')).toBeNull()
  })
})

