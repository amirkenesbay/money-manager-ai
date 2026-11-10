import { describe, expect, it } from 'vitest'
import { limitSteps, nearestStep, NO_LIMIT_STEP } from './limitSteps'

describe('limitSteps', () => {
  it('starts with no limit and grows in round tenge amounts up to a million', () => {
    const steps = limitSteps('KZT')

    expect(steps[0]).toBe(NO_LIMIT_STEP)
    expect(steps).toContain(5_000)
    expect(steps).toContain(75_000)
    expect(steps.at(-1)).toBe(1_000_000)
    expect([...steps].sort((a, b) => a - b)).toEqual(steps)
  })

  it('uses smaller steps for dollars', () => {
    expect(limitSteps('USD').at(-1)).toBe(2_000)
  })

  it('scales the range to the size of the currency', () => {
    expect(limitSteps('BYN').at(-1)).toBe(10_000)
    expect(limitSteps('UZS')[1]).toBe(100_000)
  })

  it('puts the slider on the closest step and at the start without a limit', () => {
    const steps = limitSteps('KZT')

    expect(steps[nearestStep(steps, 74_000)]).toBe(75_000)
    expect(nearestStep(steps, null)).toBe(0)
  })
})
