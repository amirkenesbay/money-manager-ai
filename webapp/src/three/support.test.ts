import { afterEach, describe, expect, it, vi } from 'vitest'
import { supports3d } from './support'

const ANDROID_AGENT = 'Mozilla/5.0 (Linux; Android 14) Telegram-Android/11.3.3 (Google Pixel 5; Android 14; SDK 34; '
const STRONG_CORES = 8

const prepare = ({ webgl = true, reducedMotion = false, agent = `${ANDROID_AGENT}HIGH)`, cores = STRONG_CORES } = {}) => {
  if (webgl) vi.stubGlobal('WebGLRenderingContext', class {})
  vi.stubGlobal('matchMedia', (query: string) => ({ matches: reducedMotion && query.includes('reduce') }))
  vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue(agent)
  vi.spyOn(navigator, 'hardwareConcurrency', 'get').mockReturnValue(cores)
}

describe('supports3d', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('allows 3D on a capable device', () => {
    prepare()

    expect(supports3d()).toBe(true)
  })

  it('falls back to flat moments without WebGL', () => {
    prepare({ webgl: false })

    expect(supports3d()).toBe(false)
  })

  it('falls back to flat moments when the user asks for less motion', () => {
    prepare({ reducedMotion: true })

    expect(supports3d()).toBe(false)
  })

  it('falls back to flat moments on a device Telegram marks as low performance', () => {
    prepare({ agent: `${ANDROID_AGENT}LOW)` })

    expect(supports3d()).toBe(false)
  })

  it('falls back to flat moments on a device with few cores', () => {
    prepare({ cores: 2 })

    expect(supports3d()).toBe(false)
  })
})
