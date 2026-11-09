import { formatDayLabel } from '../format'
import { strings } from '../strings'

export function dateChipLabel(date: string, quickDates: string[], language: string): string {
  const daysAgo = quickDates.indexOf(date)
  return daysAgo >= 0 ? strings.quickDate(daysAgo) : formatDayLabel(date, new Date(), language)
}
