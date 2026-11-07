import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { me } from '../test/fixtures'
import { updateMe } from './meApi'
import { useDeviceTimezone } from './useDeviceTimezone'

vi.mock('./meApi', () => ({ updateMe: vi.fn() }))

const TIMEZONE = 'Asia/Almaty'

describe('useDeviceTimezone', () => {
  beforeEach(() => vi.resetAllMocks())

  it('remembers the device time zone when the profile has none', async () => {
    const onMeChanged = vi.fn()
    vi.mocked(updateMe).mockResolvedValue(me({ timezone: TIMEZONE }))

    renderHook(() => useDeviceTimezone(me({ timezone: null }), onMeChanged))

    expect(updateMe).toHaveBeenCalledWith({ timezone: Intl.DateTimeFormat().resolvedOptions().timeZone })
    await waitFor(() => expect(onMeChanged).toHaveBeenCalledWith(me({ timezone: TIMEZONE })))
  })

  it('keeps a time zone the user already has', () => {
    renderHook(() => useDeviceTimezone(me({ timezone: 'Europe/Moscow' }), vi.fn()))

    expect(updateMe).not.toHaveBeenCalled()
  })
})
