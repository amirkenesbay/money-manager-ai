import { describe, expect, it } from 'vitest'
import { currentPeriodMonth, isCalendarMonth, monthRange, periodRange } from './periods'

const SALARY_DAY = 25

describe('month periods', () => {
  it('keeps the calendar month when the month starts on the first', () => {
    expect(monthRange('2026-02', 1)).toEqual({ from: '2026-02-01', to: '2026-02-28' })
    expect(isCalendarMonth(monthRange('2026-02', 1))).toBe(true)
  })

  it('runs from the start day to the day before it next month, across the new year', () => {
    expect(monthRange('2026-10', SALARY_DAY)).toEqual({ from: '2026-10-25', to: '2026-11-24' })
    expect(monthRange('2026-12', SALARY_DAY)).toEqual({ from: '2026-12-25', to: '2027-01-24' })
    expect(isCalendarMonth(monthRange('2026-10', SALARY_DAY))).toBe(false)
  })

  it('belongs to the previous month before the start day', () => {
    expect(currentPeriodMonth(new Date(2026, 9, 24), SALARY_DAY)).toBe('2026-09')
    expect(currentPeriodMonth(new Date(2026, 9, 25), SALARY_DAY)).toBe('2026-10')
    expect(currentPeriodMonth(new Date(2027, 0, 3), SALARY_DAY)).toBe('2026-12')
  })

  it('filters operations by the group month', () => {
    const today = new Date(2026, 9, 15)

    expect(periodRange('thisMonth', today, SALARY_DAY)).toEqual({ from: '2026-09-25', to: null })
    expect(periodRange('lastMonth', today, SALARY_DAY)).toEqual({ from: '2026-08-25', to: '2026-09-24' })
    expect(periodRange('last3Months', today, SALARY_DAY)).toEqual({ from: '2026-07-25', to: null })
    expect(periodRange('thisMonth', today)).toEqual({ from: '2026-10-01', to: null })
  })
})
