import { describe, expect, it } from 'vitest'
import { strings } from './strings'
import { asRendered } from './test/text'
import { formatDayLabel, formatMoney, formatSignedMoney } from './format'

const LANGUAGE = 'ru'

describe('format', () => {
  it('hides kopecks for whole amounts and keeps two digits otherwise', () => {
    expect(asRendered(formatMoney('450000', 'KZT', LANGUAGE))).toBe('450 000 ₸')
    expect(asRendered(formatMoney('12000.5', 'KZT', LANGUAGE))).toBe('12 000,50 ₸')
  })

  it('marks negative amounts with a minus sign', () => {
    expect(asRendered(formatMoney('-20000', 'KZT', LANGUAGE))).toBe('−20 000 ₸')
  })

  it('signs incomes and expenses', () => {
    expect(asRendered(formatSignedMoney('INCOME', '2000', 'USD', LANGUAGE))).toBe('+2 000 $')
    expect(asRendered(formatSignedMoney('EXPENSE', '2000', 'USD', LANGUAGE))).toBe('−2 000 $')
  })

  it('leaves zero amounts unsigned', () => {
    expect(asRendered(formatSignedMoney('INCOME', '0', 'KZT', LANGUAGE))).toBe('0 ₸')
    expect(asRendered(formatSignedMoney('EXPENSE', '0.00', 'KZT', LANGUAGE))).toBe('0 ₸')
  })

  it('names today and yesterday and spells out older days', () => {
    const today = new Date(2026, 9, 1)

    expect(formatDayLabel('2026-10-01', today, LANGUAGE)).toBe(strings.today)
    expect(formatDayLabel('2026-09-30', today, LANGUAGE)).toBe(strings.yesterday)
    expect(formatDayLabel('2026-09-20', today, LANGUAGE)).toBe('20 сентября')
  })
})
