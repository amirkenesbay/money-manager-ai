import { init, isTMA } from '@tma.js/sdk-react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { initTelegram } from './telegramEnv'

vi.mock('@tma.js/sdk-react', () => {
  const available = () => Object.assign(vi.fn(), { ifAvailable: vi.fn() })
  return {
    init: vi.fn(),
    isTMA: vi.fn(),
    retrieveRawInitData: vi.fn(),
    themeParams: { mount: available(), bindCssVars: available() },
    miniApp: { mount: available(), ready: available() },
  }
})

const isTMAMock = vi.mocked(isTMA as () => boolean)

describe('initTelegram', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('reports being outside Telegram without touching the SDK', () => {
    isTMAMock.mockReturnValue(false)

    expect(initTelegram()).toBe(false)
    expect(init).not.toHaveBeenCalled()
  })

  it('keeps the app running when the SDK fails to initialize', () => {
    isTMAMock.mockReturnValue(true)
    vi.mocked(init).mockImplementation(() => {
      throw new Error('UnknownEnvError')
    })
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)

    expect(initTelegram()).toBe(true)
  })
})
