import type { DayOfWeek, FrequencyType, Reminder, ReminderInput, ReminderSchedule } from '../api/types'
import { strings } from '../strings'

export type RepeatOption = 'daily' | 'weekly' | 'biweekly' | 'monthly' | 'yearly' | 'custom'
export type IntervalUnit = 'days' | 'weeks' | 'months' | 'years'
type Anchor = 'none' | 'weekday' | 'monthDay' | 'yearDay'
type FixedRepeat = Exclude<RepeatOption, 'custom'>

export const REPEAT_OPTIONS: RepeatOption[] = ['daily', 'weekly', 'biweekly', 'monthly', 'yearly', 'custom']
export const INTERVAL_UNITS: IntervalUnit[] = ['days', 'weeks', 'months', 'years']
export const WEEKDAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
export const MAX_INTERVAL: Record<IntervalUnit, number> = { days: 365, weeks: 52, months: 24, years: 10 }
export const MONTH_COUNT = 12
export const MAX_DAY_OF_MONTH = 31

const FIXED_FREQUENCY: Record<FixedRepeat, FrequencyType> = {
  daily: 'DAILY',
  weekly: 'WEEKLY',
  biweekly: 'BIWEEKLY',
  monthly: 'MONTHLY',
  yearly: 'YEARLY',
}

const CUSTOM_FREQUENCY: Record<IntervalUnit, FrequencyType> = {
  days: 'CUSTOM_DAYS',
  weeks: 'CUSTOM_WEEKS',
  months: 'CUSTOM_MONTHS',
  years: 'CUSTOM_YEARS',
}

const ANCHORS: Record<FrequencyType, Anchor> = {
  DAILY: 'none',
  CUSTOM_DAYS: 'none',
  WEEKLY: 'weekday',
  BIWEEKLY: 'weekday',
  CUSTOM_WEEKS: 'weekday',
  MONTHLY: 'monthDay',
  BIMONTHLY: 'monthDay',
  CUSTOM_MONTHS: 'monthDay',
  YEARLY: 'yearDay',
  CUSTOM_YEARS: 'yearDay',
}

const BIMONTHLY_INTERVAL = 2
const DEFAULT_INTERVAL = 2
const DEFAULT_HOUR = 21
const MINUTE_DIGITS = 2
const SAMPLE_YEAR = 2024

export interface ScheduleDraft {
  repeat: RepeatOption
  unit: IntervalUnit
  interval: number
  weekday: DayOfWeek
  dayOfMonth: number
  month: number
  hour: number
  minute: number
}

export const DEFAULT_SCHEDULE: ScheduleDraft = {
  repeat: 'daily',
  unit: 'days',
  interval: DEFAULT_INTERVAL,
  weekday: 'MONDAY',
  dayOfMonth: 1,
  month: 1,
  hour: DEFAULT_HOUR,
  minute: 0,
}

export function draftOf(reminder: Reminder | null): ScheduleDraft {
  if (reminder === null) return DEFAULT_SCHEDULE
  return {
    ...repeatOf(reminder),
    weekday: reminder.dayOfWeek ?? DEFAULT_SCHEDULE.weekday,
    dayOfMonth: reminder.dayOfMonth ?? DEFAULT_SCHEDULE.dayOfMonth,
    month: reminder.monthOfYear ?? DEFAULT_SCHEDULE.month,
    hour: reminder.hour,
    minute: reminder.minute,
  }
}

function repeatOf(reminder: ReminderSchedule): Pick<ScheduleDraft, 'repeat' | 'unit' | 'interval'> {
  const fixed = (Object.keys(FIXED_FREQUENCY) as FixedRepeat[]).find((repeat) => FIXED_FREQUENCY[repeat] === reminder.frequencyType)
  if (fixed) return { repeat: fixed, unit: DEFAULT_SCHEDULE.unit, interval: DEFAULT_INTERVAL }
  if (reminder.frequencyType === 'BIMONTHLY') return { repeat: 'custom', unit: 'months', interval: BIMONTHLY_INTERVAL }
  const unit = INTERVAL_UNITS.find((candidate) => CUSTOM_FREQUENCY[candidate] === reminder.frequencyType) ?? DEFAULT_SCHEDULE.unit
  return { repeat: 'custom', unit, interval: reminder.customInterval ?? DEFAULT_INTERVAL }
}

export const frequencyOf = (draft: ScheduleDraft): FrequencyType =>
  draft.repeat === 'custom' ? CUSTOM_FREQUENCY[draft.unit] : FIXED_FREQUENCY[draft.repeat]

export const anchorOf = (frequency: FrequencyType): Anchor => ANCHORS[frequency]

export const intervalValid = (draft: ScheduleDraft): boolean =>
  draft.repeat !== 'custom' || (Number.isInteger(draft.interval) && draft.interval >= 1 && draft.interval <= MAX_INTERVAL[draft.unit])

export function toReminderInput(name: string, icon: string | null, draft: ScheduleDraft): ReminderInput {
  const frequencyType = frequencyOf(draft)
  const anchor = anchorOf(frequencyType)
  return {
    name,
    icon,
    frequencyType,
    customInterval: draft.repeat === 'custom' ? draft.interval : null,
    hour: draft.hour,
    minute: draft.minute,
    dayOfWeek: anchor === 'weekday' ? draft.weekday : null,
    dayOfMonth: anchor === 'monthDay' || anchor === 'yearDay' ? draft.dayOfMonth : null,
    monthOfYear: anchor === 'yearDay' ? draft.month : null,
  }
}

export function describeSchedule(schedule: ReminderSchedule, language: string): string {
  return strings.scheduleAt(describeRepeat(schedule, language), formatTime(schedule.hour, schedule.minute))
}

function describeRepeat(schedule: ReminderSchedule, language: string): string {
  const repeat = repeatOf(schedule)
  const every = repeat.repeat === 'custom' ? strings.scheduleEvery(repeat.interval, repeat.unit) : fixedRepeatLabel(repeat.repeat)
  const anchor = anchorLabel(schedule, language)
  return anchor === null ? every : strings.scheduleWithAnchor(every, anchor)
}

function fixedRepeatLabel(repeat: FixedRepeat): string {
  const labels: Record<FixedRepeat, string> = {
    daily: strings.scheduleDaily,
    weekly: strings.scheduleEveryWeek,
    biweekly: strings.scheduleEveryTwoWeeks,
    monthly: strings.scheduleEveryMonth,
    yearly: strings.scheduleEveryYear,
  }
  return labels[repeat]
}

function anchorLabel(schedule: ReminderSchedule, language: string): string | null {
  switch (anchorOf(schedule.frequencyType)) {
    case 'weekday':
      return schedule.dayOfWeek === null ? null : weekdayName(schedule.dayOfWeek, language)
    case 'monthDay':
      return schedule.dayOfMonth === null ? null : strings.dayOfMonthAnchor(schedule.dayOfMonth)
    case 'yearDay':
      return schedule.dayOfMonth === null || schedule.monthOfYear === null ? null : dayAndMonth(schedule.dayOfMonth, schedule.monthOfYear, language)
    default:
      return null
  }
}

export function weekdayName(weekday: DayOfWeek, language: string): string {
  const mondayFirst = new Date(SAMPLE_YEAR, 0, 1 + WEEKDAYS.indexOf(weekday))
  return new Intl.DateTimeFormat(language, { weekday: 'short' }).format(mondayFirst).replace('.', '')
}

export function monthName(month: number, language: string): string {
  return new Intl.DateTimeFormat(language, { month: 'long' }).format(new Date(SAMPLE_YEAR, month - 1, 1))
}

function dayAndMonth(day: number, month: number, language: string): string {
  return new Intl.DateTimeFormat(language, { day: 'numeric', month: 'long' }).format(new Date(SAMPLE_YEAR, month - 1, day))
}

export function formatTime(hour: number, minute: number): string {
  const pad = (value: number) => String(value).padStart(MINUTE_DIGITS, '0')
  return `${pad(hour)}:${pad(minute)}`
}

export function parseTime(value: string): { hour: number; minute: number } | null {
  const [hour, minute] = value.split(':').map(Number)
  return Number.isInteger(hour) && Number.isInteger(minute) ? { hour, minute } : null
}
