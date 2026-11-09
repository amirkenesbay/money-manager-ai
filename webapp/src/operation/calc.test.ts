import { describe, expect, it } from 'vitest'
import { amountOfExpression, displayExpression, evaluateExpression, hasOperator, normalizeExpression } from './calc'

describe('calc', () => {
  it('adds up a few prices', () => {
    expect(evaluateExpression('1120+350')).toBe(1470)
  })

  it('multiplies and divides before adding', () => {
    expect(evaluateExpression('500+3×200')).toBe(1100)
    expect(evaluateExpression('9000÷3−1000')).toBe(2000)
  })

  it('understands operators typed on a physical keyboard', () => {
    expect(normalizeExpression('1 200 * 3 - 600 / 2')).toBe('1200×3−600÷2')
  })

  it('keeps only the last of two operators in a row and ignores a leading one', () => {
    expect(normalizeExpression('+500+−200')).toBe('500−200')
  })

  it('ignores a trailing operator while the user is still typing', () => {
    expect(evaluateExpression('1500+')).toBe(1500)
  })

  it('refuses division by zero and empty input', () => {
    expect(evaluateExpression('100÷0')).toBeNull()
    expect(evaluateExpression('')).toBeNull()
  })

  it('accepts a decimal comma', () => {
    expect(evaluateExpression('12,50+0,25')).toBe(12.75)
  })

  it('gives an amount only when the result is positive', () => {
    expect(amountOfExpression('1000+250')).toBe('1250')
    expect(amountOfExpression('12,5×2')).toBe('25')
    expect(amountOfExpression('100−200')).toBeNull()
  })

  it('shows the expression with spaced digit groups', () => {
    expect(displayExpression('1120+35000')).toBe('1 120 + 35 000')
    expect(hasOperator('1120+35000')).toBe(true)
    expect(hasOperator('1120')).toBe(false)
  })
})
