import type { RecurrenceUnit, Repeat } from '../api/types'
import type { IntervalUnit } from '../reminders/schedule'
import { strings } from '../strings'

export const RECURRENCE_UNITS: RecurrenceUnit[] = ['DAY', 'WEEK', 'MONTH', 'YEAR']
export const REPEAT_CHOICES: (RecurrenceUnit | null)[] = [null, 'WEEK', 'MONTH', 'YEAR']

const INTERVAL_UNITS: Record<RecurrenceUnit, IntervalUnit> = { DAY: 'days', WEEK: 'weeks', MONTH: 'months', YEAR: 'years' }

export const intervalUnitOf = (unit: RecurrenceUnit): IntervalUnit => INTERVAL_UNITS[unit]

export function describeRepeat({ unit, interval }: Repeat): string {
  if (interval !== 1) return strings.scheduleEvery(interval, intervalUnitOf(unit))
  const every: Record<RecurrenceUnit, string> = {
    DAY: strings.scheduleDaily,
    WEEK: strings.scheduleEveryWeek,
    MONTH: strings.scheduleEveryMonth,
    YEAR: strings.scheduleEveryYear,
  }
  return every[unit]
}

export function repeatChoiceLabel(choice: RecurrenceUnit | null): string {
  return choice === null ? strings.repeatNone : describeRepeat({ unit: choice, interval: 1 })
}
