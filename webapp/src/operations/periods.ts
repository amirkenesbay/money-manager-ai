import { shiftMonth, toIsoDate, toIsoMonth } from '../format'

export type Period = 'last30Days' | 'thisMonth' | 'lastMonth' | 'last3Months' | 'allTime'

export const PERIODS: Period[] = ['last30Days', 'thisMonth', 'lastMonth', 'last3Months', 'allTime']

export const DEFAULT_PERIOD: Period = 'last30Days'

const RECENT_DAYS = 29
const QUARTER_MONTHS = 3
export const CALENDAR_MONTH_START = 1

export interface DateRange {
  from: string | null
  to: string | null
}

export interface MonthRange {
  from: string
  to: string
}

export function monthRange(isoMonth: string, startDay: number): MonthRange {
  const [year, month] = isoMonth.split('-').map(Number)
  return {
    from: toIsoDate(new Date(year, month - 1, startDay)),
    to: toIsoDate(new Date(year, month, startDay - 1)),
  }
}

export function isCalendarMonth(range: MonthRange): boolean {
  return Number(range.from.slice(-2)) === CALENDAR_MONTH_START
}

export function currentPeriodMonth(today: Date, startDay: number): string {
  const month = toIsoMonth(today)
  return today.getDate() >= startDay ? month : shiftMonth(month, -1)
}

export function periodRange(period: Period, today: Date, startDay: number = CALENDAR_MONTH_START): DateRange {
  const current = currentPeriodMonth(today, startDay)
  switch (period) {
    case 'last30Days':
      return { from: toIsoDate(new Date(today.getFullYear(), today.getMonth(), today.getDate() - RECENT_DAYS)), to: null }
    case 'thisMonth':
      return { from: monthRange(current, startDay).from, to: null }
    case 'lastMonth':
      return monthRange(shiftMonth(current, -1), startDay)
    case 'last3Months':
      return { from: monthRange(shiftMonth(current, 1 - QUARTER_MONTHS), startDay).from, to: null }
    case 'allTime':
      return { from: null, to: null }
  }
}
