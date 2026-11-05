import { parseLocalDate, toIsoDate } from '../format'

const DAY_MS = 86_400_000
const WEEK_DAYS = 7
const CALENDAR_YEARS_AHEAD = 1

export type DueChoice = 'none' | 'week' | 'month' | 'pick'

export const DUE_CHOICES: DueChoice[] = ['none', 'week', 'month', 'pick']

export function quickDueDate(choice: DueChoice, today: Date): string | null {
  if (choice === 'week') return toIsoDate(new Date(today.getFullYear(), today.getMonth(), today.getDate() + WEEK_DAYS))
  if (choice === 'month') return toIsoDate(new Date(today.getFullYear(), today.getMonth() + 1, today.getDate()))
  return null
}

export const latestDueDate = (today: Date): string =>
  toIsoDate(new Date(today.getFullYear() + CALENDAR_YEARS_AHEAD, today.getMonth(), today.getDate()))

export function overdueDays(dueDate: string | null, today: Date): number {
  if (!dueDate) return 0
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return Math.max(0, Math.round((start.getTime() - parseLocalDate(dueDate).getTime()) / DAY_MS))
}
