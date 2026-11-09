import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { useHiddenAmounts } from './useHiddenAmounts'

describe('useHiddenAmounts', () => {
  beforeEach(() => localStorage.clear())

  it('remembers hidden amounts for the next visit', () => {
    const first = renderHook(() => useHiddenAmounts())

    act(() => first.result.current.toggle())

    expect(first.result.current.hidden).toBe(true)
    expect(renderHook(() => useHiddenAmounts()).result.current.hidden).toBe(true)
  })

  it('shows amounts again after a second tap', () => {
    const { result } = renderHook(() => useHiddenAmounts())

    act(() => result.current.toggle())
    act(() => result.current.toggle())

    expect(result.current.hidden).toBe(false)
  })
})
