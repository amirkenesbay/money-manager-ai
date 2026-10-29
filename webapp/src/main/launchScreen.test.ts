import { describe, expect, it } from 'vitest'
import { launchScreen } from './launchScreen'

describe('launchScreen', () => {
  it('reads the screen the bot asked to open', () => {
    expect(launchScreen('?screen=add')).toBe('add')
    expect(launchScreen('?mock=empty&screen=categories')).toBe('categories')
  })

  it('opens home for a missing or unknown screen', () => {
    expect(launchScreen('')).toBeNull()
    expect(launchScreen('?screen=settings')).toBeNull()
  })
})
