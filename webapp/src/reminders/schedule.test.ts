import { describe, expect, it } from 'vitest'
import { reminder } from '../test/fixtures'
import { DEFAULT_SCHEDULE, describeSchedule, draftOf, intervalValid, toReminderInput } from './schedule'

const LANGUAGE = 'ru'

describe('reminder schedule', () => {
  it('sends only the fields the repeat uses', () => {
    const input = toReminderInput('Итоги недели', '📊', { ...DEFAULT_SCHEDULE, repeat: 'weekly', weekday: 'SUNDAY', dayOfMonth: 5, hour: 20 })

    expect(input).toMatchObject({ frequencyType: 'WEEKLY', dayOfWeek: 'SUNDAY', dayOfMonth: null, monthOfYear: null, customInterval: null, hour: 20 })
  })

  it('turns a custom repeat into the matching frequency with its interval', () => {
    const input = toReminderInput('Аренда', null, { ...DEFAULT_SCHEDULE, repeat: 'custom', unit: 'months', interval: 3, dayOfMonth: 10 })

    expect(input).toMatchObject({ frequencyType: 'CUSTOM_MONTHS', customInterval: 3, dayOfMonth: 10, dayOfWeek: null })
  })

  it('opens a bimonthly reminder from the bot as every 2 months', () => {
    expect(draftOf(reminder({ frequencyType: 'BIMONTHLY', dayOfMonth: 15 }))).toMatchObject({ repeat: 'custom', unit: 'months', interval: 2, dayOfMonth: 15 })
  })

  it('rejects an interval outside the bot limits', () => {
    expect(intervalValid({ ...DEFAULT_SCHEDULE, repeat: 'custom', unit: 'years', interval: 11 })).toBe(false)
    expect(intervalValid({ ...DEFAULT_SCHEDULE, repeat: 'custom', unit: 'days', interval: 365 })).toBe(true)
  })

  it('describes schedules in words with the time', () => {
    expect(describeSchedule(reminder(), LANGUAGE)).toBe('каждый день · 21:00')
    expect(describeSchedule(reminder({ frequencyType: 'MONTHLY', dayOfMonth: 5, hour: 10 }), LANGUAGE)).toBe('каждый месяц · 5-го · 10:00')
    expect(describeSchedule(reminder({ frequencyType: 'CUSTOM_DAYS', customInterval: 3, hour: 9, minute: 30 }), LANGUAGE)).toBe('раз в 3 дня · 09:30')
    expect(describeSchedule(reminder({ frequencyType: 'WEEKLY', dayOfWeek: 'SUNDAY' }), LANGUAGE)).toBe('каждую неделю · вс · 21:00')
  })
})
