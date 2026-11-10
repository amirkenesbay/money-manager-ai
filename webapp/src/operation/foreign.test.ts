import { describe, expect, it } from 'vitest'
import { asRendered } from '../test/text'
import { convertAmount, lastCurrency, parseRate, rateText, rememberCurrency, rememberedRate, rememberRate } from './foreign'

describe('foreign amounts', () => {
  it('converts to cents', () => {
    expect(convertAmount('100', 521.6)).toBe('52160')
    expect(convertAmount('9000', 1 / 484.3)).toBe('18.58')
  })

  it('names the rate from the dearer currency', () => {
    expect(rateText(521.6, 'EUR', 'KZT', 'ru')).toBe(asRendered('1 € = 521,6 ₸'))
    expect(rateText(0.038, 'UZS', 'KZT', 'ru')).toBe(asRendered('1 ₸ = 26,32 сўм'))
  })

  it('reads a typed rate with a comma and many digits', () => {
    expect(parseRate('0,0381')).toBe(0.0381)
    expect(parseRate('0')).toBeNull()
    expect(parseRate('abc')).toBeNull()
  })

  it('remembers the currency and rate per group', () => {
    rememberCurrency('g1', 'EUR')
    rememberRate('g1', 'EUR', 515)

    expect(lastCurrency('g1')).toBe('EUR')
    expect(rememberedRate('g1', 'EUR')).toBe(515)
    expect(lastCurrency('g2')).toBeNull()

    rememberRate('g1', 'EUR', null)
    expect(rememberedRate('g1', 'EUR')).toBeNull()
  })
})
