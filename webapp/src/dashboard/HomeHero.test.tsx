import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { strings } from '../strings'
import { balance, summary } from '../test/fixtures'
import { HomeHero } from './HomeHero'

const AUTO_ADVANCE_MS = 6000

describe('HomeHero', () => {
  const scrollTo = vi.fn()

  beforeEach(() => {
    vi.useFakeTimers()
    Element.prototype.scrollTo = scrollTo
    localStorage.clear()
  })

  afterEach(() => {
    vi.useRealTimers()
    scrollTo.mockReset()
  })

  const renderHero = () => render(<HomeHero balance={balance()} summary={summary()} language="ru" onAddIncome={vi.fn()} />)

  it('turns to the next slide on its own', () => {
    renderHero()

    act(() => vi.advanceTimersByTime(AUTO_ADVANCE_MS))

    expect(scrollTo).toHaveBeenCalledTimes(1)
  })

  it('waits while the user is touching the summary', () => {
    renderHero()

    fireEvent.pointerDown(screen.getByRole('region', { name: strings.homeSummaryLabel }))
    act(() => vi.advanceTimersByTime(AUTO_ADVANCE_MS))

    expect(scrollTo).not.toHaveBeenCalled()
  })
})
