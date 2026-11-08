import { describe, expect, it } from 'vitest'
import { endOfSpeech } from './endOfSpeech'

const CHUNK_MS = 100
const QUIET = 0.003
const VOICE = 0.1

const feed = (levels: number[]) => {
  const detector = endOfSpeech()
  return levels.map((level) => detector.hear(level, CHUNK_MS))
}

const repeat = (level: number, chunks: number) => Array<number>(chunks).fill(level)

describe('endOfSpeech', () => {
  it('ends after the user speaks and then stays quiet for a moment', () => {
    const ended = feed([...repeat(QUIET, 5), ...repeat(VOICE, 10), ...repeat(QUIET, 12)])

    expect(ended.at(-1)).toBe(true)
    expect(ended.slice(0, -1)).not.toContain(true)
  })

  it('keeps listening through short pauses between words', () => {
    const ended = feed([...repeat(QUIET, 3), ...repeat(VOICE, 5), ...repeat(QUIET, 6), ...repeat(VOICE, 5), ...repeat(QUIET, 6)])

    expect(ended).not.toContain(true)
  })

  it('still hears the end when the user speaks from the very first moment', () => {
    expect(feed([...repeat(VOICE, 10), ...repeat(QUIET, 12)]).at(-1)).toBe(true)
  })

  it('does not end before the user starts speaking', () => {
    expect(feed(repeat(QUIET, 30))).not.toContain(true)
  })

  it('gives up when nobody speaks for a long time', () => {
    expect(feed(repeat(QUIET, 80)).at(-1)).toBe(true)
  })

  it('treats a steady noisy room as silence', () => {
    const noisy = 0.03
    const ended = feed([...repeat(noisy, 10), ...repeat(0.2, 8), ...repeat(noisy, 12)])

    expect(ended.at(-1)).toBe(true)
  })
})
