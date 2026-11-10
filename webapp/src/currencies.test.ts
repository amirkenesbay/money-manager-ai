import { describe, expect, it } from 'vitest'
import { CURRENCIES, CURRENCY_CATALOG, currencyDisplayName } from './currencies'

describe('currencies', () => {
  it('offers the Belarusian ruble next to the old five', () => {
    expect(CURRENCIES).toEqual(expect.arrayContaining(['KZT', 'USD', 'RUB', 'EUR', 'KGS', 'BYN']))
    expect(CURRENCY_CATALOG.BYN.symbol).toBe('Br')
  })

  it('names a currency in the app language so two rubles differ', () => {
    expect(currencyDisplayName('BYN', 'ru')).toMatch(/^Белорусский рубль/)
    expect(currencyDisplayName('RUB', 'ru')).toMatch(/^Российский рубль/)
    expect(currencyDisplayName('KZT', 'en')).toMatch(/tenge/i)
  })
})
