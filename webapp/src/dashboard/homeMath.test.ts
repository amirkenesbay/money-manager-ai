import { describe, expect, it } from 'vitest'
import { summary } from '../test/fixtures'
import { dayBars, daysLeft, monthPace, periodDates, safeToSpend } from './homeMath'

const TODAY = new Date(2026, 9, 5)

describe('home math', () => {
  it('lists every day of a period that crosses a month boundary', () => {
    expect(periodDates('2026-09-28', '2026-10-02')).toEqual(['2026-09-28', '2026-09-29', '2026-09-30', '2026-10-01', '2026-10-02'])
  })

  it('places daily spending on its day and marks today and the days ahead', () => {
    const bars = dayBars(summary(), TODAY)

    expect(bars).toHaveLength(31)
    expect(bars[1]).toEqual({ date: '2026-10-02', total: 12000, isToday: false, isFuture: false })
    expect(bars[4]).toEqual({ date: '2026-10-05', total: 3000, isToday: true, isFuture: false })
    expect(bars[5].isFuture).toBe(true)
  })

  it('counts today among the days left', () => {
    expect(daysLeft(summary(), TODAY)).toBe(27)
  })

  it('splits what is left of the income over the remaining days', () => {
    expect(safeToSpend(summary({ income: '300000', expense: '30000' }), TODAY)).toEqual({ perDay: 10000, left: 270000, daysLeft: 27 })
  })

  it('cannot tell a daily amount without income', () => {
    expect(safeToSpend(summary({ income: '0' }), TODAY)).toBeNull()
  })

  it('never suggests spending below zero once the income is gone', () => {
    expect(safeToSpend(summary({ income: '10000', expense: '12000' }), TODAY)?.perDay).toBe(0)
  })

  it('compares the share of income spent with the share of the month gone', () => {
    expect(monthPace(summary({ income: '300000', expense: '150000' }), TODAY)).toEqual({ spentPercent: 50, elapsedPercent: 16 })
  })
})
