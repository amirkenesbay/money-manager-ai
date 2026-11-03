import type { MonthSummary } from '../api/types'
import { parseLocalDate, toIsoDate } from '../format'
import { FULL_PERCENT, percentOf } from '../percent'

export interface DayBar {
  date: string
  total: number
  isToday: boolean
  isFuture: boolean
}

export interface SafeToSpend {
  perDay: number
  left: number
  daysLeft: number
}

export interface MonthPace {
  spentPercent: number
  elapsedPercent: number
}

const nextDay = (date: Date): Date => new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1)

export function periodDates(from: string, to: string): string[] {
  const dates: string[] = []
  for (let day = parseLocalDate(from); toIsoDate(day) <= to; day = nextDay(day)) dates.push(toIsoDate(day))
  return dates
}

export function dayBars(summary: MonthSummary, today: Date): DayBar[] {
  const todayIso = toIsoDate(today)
  const totals = new Map(summary.dailyExpense.map((day) => [day.date, Number(day.total)]))
  return periodDates(summary.from, summary.to).map((date) => ({
    date,
    total: totals.get(date) ?? 0,
    isToday: date === todayIso,
    isFuture: date > todayIso,
  }))
}

export function daysLeft(summary: MonthSummary, today: Date): number {
  return periodDates(summary.from, summary.to).filter((date) => date >= toIsoDate(today)).length
}

export function safeToSpend(summary: MonthSummary, today: Date): SafeToSpend | null {
  if (Number(summary.income) <= 0) return null
  const left = Number(summary.income) - Number(summary.expense)
  const remainingDays = Math.max(1, daysLeft(summary, today))
  return { perDay: Math.max(0, Math.floor(left / remainingDays)), left, daysLeft: remainingDays }
}

export function monthPace(summary: MonthSummary, today: Date): MonthPace {
  const total = periodDates(summary.from, summary.to).length
  const elapsed = total - daysLeft(summary, today) + 1
  return {
    spentPercent: Math.round(percentOf(Number(summary.expense), Number(summary.income))),
    elapsedPercent: Math.min(FULL_PERCENT, Math.round(percentOf(elapsed, total))),
  }
}
