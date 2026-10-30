import { afterEach, describe, expect, it, vi } from 'vitest'
import { withMotion } from './motion'

const stubTransitions = () => {
  const startViewTransition = vi.fn((update: () => void) => {
    update()
    return { ready: Promise.resolve(), finished: Promise.resolve() }
  })
  Object.defineProperty(document, 'startViewTransition', { value: startViewTransition, configurable: true })
  return startViewTransition
}

describe('withMotion', () => {
  afterEach(() => {
    Reflect.deleteProperty(document, 'startViewTransition')
    delete document.documentElement.dataset.motion
    vi.unstubAllGlobals()
  })

  it('applies the change at once when the browser has no view transitions', () => {
    const update = vi.fn()

    withMotion('forward', update)

    expect(update).toHaveBeenCalledOnce()
  })

  it('animates the change in the requested direction', () => {
    const startViewTransition = stubTransitions()
    vi.stubGlobal('matchMedia', () => ({ matches: false }))
    const update = vi.fn()

    withMotion('back', update)

    expect(startViewTransition).toHaveBeenCalledOnce()
    expect(update).toHaveBeenCalledOnce()
    expect(document.documentElement.dataset.motion).toBe('back')
  })

  it('skips the animation when the user asks for less motion', () => {
    const startViewTransition = stubTransitions()
    vi.stubGlobal('matchMedia', (query: string) => ({ matches: query.includes('reduce') }))
    const update = vi.fn()

    withMotion('forward', update)

    expect(startViewTransition).not.toHaveBeenCalled()
    expect(update).toHaveBeenCalledOnce()
  })
})
