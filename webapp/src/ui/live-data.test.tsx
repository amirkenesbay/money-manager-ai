import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { groupAmountInput } from '../operation/amount'
import { FlapNumber } from './FlapNumber'

describe('live data', () => {
  it('groups thousands while the amount is typed', () => {
    expect(groupAmountInput('1500')).toBe('1 500')
    expect(groupAmountInput('1 5000')).toBe('15 000')
    expect(groupAmountInput('1234567,5')).toBe('1 234 567,5')
    expect(groupAmountInput('12a')).toBe('12a')
  })

  it('flips only the digits that changed', () => {
    const view = render(<FlapNumber text="84 500 ₸" memoryKey="spent" />)
    view.rerender(<FlapNumber text="88 000 ₸" memoryKey="spent" />)

    const flipped = Array.from(view.container.querySelectorAll('.flap-digit.flip')).map((digit) => digit.textContent)
    expect(flipped).toEqual(['8', '0'])
    expect(screen.getByText('88 000 ₸')).toBeInTheDocument()
  })

  it('remembers the last amount across remounts', () => {
    render(<FlapNumber text="1 000 ₸" memoryKey="remount" />).unmount()
    const view = render(<FlapNumber text="1 500 ₸" memoryKey="remount" />)

    expect(view.container.querySelectorAll('.flap-digit.flip')).toHaveLength(1)
  })
})
