import { describe, expect, it } from 'vitest'
import { balance } from '../test/fixtures'
import { initialForCurrent } from './balanceMath'

describe('initialForCurrent', () => {
  it('finds the starting amount that gives the balance the user has now', () => {
    const now = balance({ initial: '50000', income: '450000', expense: '76950.50', total: '423049.50' })

    expect(initialForCurrent(now, '400000')).toBe('26950.50')
  })
})
