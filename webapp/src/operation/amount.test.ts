import { describe, expect, it } from 'vitest'
import { parseAmount } from './amount'

describe('parseAmount', () => {
  it('accepts spaces and a decimal comma', () => {
    expect(parseAmount('5 000')).toBe('5000')
    expect(parseAmount('1 250,5')).toBe('1250.5')
  })

  it('rejects zero, negatives, letters and extra decimals', () => {
    expect(parseAmount('0')).toBeNull()
    expect(parseAmount('-5')).toBeNull()
    expect(parseAmount('5к')).toBeNull()
    expect(parseAmount('1.234')).toBeNull()
    expect(parseAmount('')).toBeNull()
  })
})
