import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { me } from '../test/fixtures'
import { updateMe } from './meApi'
import { deviceTimezone, describeTimezone } from './timezone'
import { useDeviceTimezone } from './useDeviceTimezone'

vi.mock('./meApi', () => ({ updateMe: vi.fn() }))
vi.mock('./timezone', async (importOriginal) => ({ ...(await importOriginal<typeof import('./timezone')>()), deviceTimezone: vi.fn() }))

const ASTANA = 'Asia/Almaty'
const BALI = 'Asia/Makassar'

describe('useDeviceTimezone', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(deviceTimezone).mockReturnValue(BALI)
  })

  it('follows the phone to a new time zone after a trip', async () => {
    const onMeChanged = vi.fn()
    vi.mocked(updateMe).mockResolvedValue(me({ timezone: BALI }))

    renderHook(() => useDeviceTimezone(me({ timezone: ASTANA }), onMeChanged))

    expect(updateMe).toHaveBeenCalledWith({ timezone: BALI })
    await waitFor(() => expect(onMeChanged).toHaveBeenCalledWith(me({ timezone: BALI })))
  })

  it('remembers the phone time zone when the profile has none', () => {
    vi.mocked(updateMe).mockResolvedValue(me({ timezone: BALI }))

    renderHook(() => useDeviceTimezone(me({ timezone: null }), vi.fn()))

    expect(updateMe).toHaveBeenCalledWith({ timezone: BALI })
  })

  it('leaves the server alone when the zone already matches the phone', () => {
    renderHook(() => useDeviceTimezone(me({ timezone: BALI }), vi.fn()))

    expect(updateMe).not.toHaveBeenCalled()
  })
})

describe('describeTimezone', () => {
  it('names the city and its offset from UTC', () => {
    expect(describeTimezone(BALI)).toBe('Makassar (UTC+8)')
    expect(describeTimezone('America/New_York')).toMatch(/^New York \(UTC-[45]\)$/)
  })
})
