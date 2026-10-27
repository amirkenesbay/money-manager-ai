import { init, isTMA, miniApp, swipeBehavior, viewport } from '@tma.js/sdk-react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { initTelegram } from './telegramEnv'

const { STABLE_HEIGHT } = vi.hoisted(() => ({ STABLE_HEIGHT: 640 }))

vi.mock('@tma.js/sdk-react', () => {
  const available = () => Object.assign(vi.fn(), { ifAvailable: vi.fn(() => ({ ok: true, data: undefined })) })
  const signal = <T,>(value: T) => Object.assign(vi.fn(() => value), { sub: vi.fn() })
  return {
    init: vi.fn(),
    isTMA: vi.fn(),
    retrieveRawInitData: vi.fn(),
    themeParams: { mount: available() },
    backButton: { mount: available() },
    swipeBehavior: { mount: available(), disableVertical: available() },
    viewport: { mount: available(), expand: available(), stableHeight: signal(STABLE_HEIGHT) },
    miniApp: {
      mount: available(),
      ready: available(),
      isDark: signal(true),
      setHeaderColor: available(),
      setBgColor: available(),
      setBottomBarColor: available(),
    },
  }
})

const isTMAMock = vi.mocked(isTMA as () => boolean)

describe('initTelegram', () => {
  afterEach(() => {
    vi.clearAllMocks()
    vi.restoreAllMocks()
  })

  it('reports being outside Telegram without touching the SDK', () => {
    isTMAMock.mockReturnValue(false)

    expect(initTelegram()).toBe(false)
    expect(init).not.toHaveBeenCalled()
  })

  it('keeps the app running when the SDK fails to initialize', () => {
    isTMAMock.mockReturnValue(true)
    vi.mocked(init).mockImplementationOnce(() => {
      throw new Error('UnknownEnvError')
    })
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)

    expect(initTelegram()).toBe(true)
  })

  it('follows the Telegram color scheme and paints Telegram around the app', () => {
    isTMAMock.mockReturnValue(true)

    initTelegram()

    expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
    expect(miniApp.setHeaderColor.ifAvailable).toHaveBeenCalled()
    expect(miniApp.setBgColor.ifAvailable).toHaveBeenCalled()
    expect(miniApp.setBottomBarColor.ifAvailable).toHaveBeenCalled()
    expect(miniApp.isDark.sub).toHaveBeenCalled()
  })

  it('expands, locks vertical swipes and uses the stable viewport height', async () => {
    isTMAMock.mockReturnValue(true)

    initTelegram()

    expect(swipeBehavior.disableVertical.ifAvailable).toHaveBeenCalled()
    await vi.waitFor(() => expect(viewport.expand.ifAvailable).toHaveBeenCalled())
    expect(document.documentElement.style.getPropertyValue('--app-height')).toBe(`${STABLE_HEIGHT}px`)
  })
})
