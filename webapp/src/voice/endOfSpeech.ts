const MIN_SPEECH_LEVEL = 0.02
const NOISE_MULTIPLIER = 3
const MAX_NOISE_LEVEL = 0.03
const CALIBRATION_MS = 300
const SPEECH_START_MS = 150
const SPEECH_END_SILENCE_MS = 1200
const NO_SPEECH_TIMEOUT_MS = 8000

export interface EndOfSpeech {
  hear: (level: number, durationMs: number) => boolean
}

export function endOfSpeech(): EndOfSpeech {
  let noise = Number.POSITIVE_INFINITY
  let voicedMs = 0
  let silentMs = 0
  let elapsedMs = 0
  return {
    hear: (level, durationMs) => {
      elapsedMs += durationMs
      if (elapsedMs <= CALIBRATION_MS) {
        noise = Math.min(noise, level, MAX_NOISE_LEVEL)
        return false
      }
      const loud = level >= Math.max(MIN_SPEECH_LEVEL, noise * NOISE_MULTIPLIER)
      if (voicedMs < SPEECH_START_MS) {
        voicedMs = loud ? voicedMs + durationMs : 0
        return elapsedMs >= NO_SPEECH_TIMEOUT_MS
      }
      silentMs = loud ? 0 : silentMs + durationMs
      return silentMs >= SPEECH_END_SILENCE_MS
    },
  }
}
