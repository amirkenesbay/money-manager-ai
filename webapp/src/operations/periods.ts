import { toIsoDate } from '../format'

export type Period = 'last30Days' | 'thisMonth' | 'lastMonth' | 'last3Months' | 'allTime'

export const PERIODS: Period[] = ['last30Days', 'thisMonth', 'lastMonth', 'last3Months', 'allTime']

export const DEFAULT_PERIOD: Period = 'last30Days'

const RECENT_DAYS = 29
const QUARTER_MONTHS = 3

export interface DateRange {
  from: string | null
  to: string | null
}

export function periodRange(period: Period, today: Date): DateRange {
  const year = today.getFullYear()
  const month = today.getMonth()
  switch (period) {
    case 'last30Days':
      return { from: toIsoDate(new Date(year, month, today.getDate() - RECENT_DAYS)), to: null }
    case 'thisMonth':
      return { from: toIsoDate(new Date(year, month, 1)), to: null }
    case 'lastMonth':
      return { from: toIsoDate(new Date(year, month - 1, 1)), to: toIsoDate(new Date(year, month, 0)) }
    case 'last3Months':
      return { from: toIsoDate(new Date(year, month - QUARTER_MONTHS + 1, 1)), to: null }
    case 'allTime':
      return { from: null, to: null }
  }
}
