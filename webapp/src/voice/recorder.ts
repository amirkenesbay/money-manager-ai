import { downsample, encodeWav, isSilent, joinChunks, rms, WAV_MIME_TYPE } from './wav'

const BUFFER_SIZE = 4096
const FIRST_SOUND_TIMEOUT_MS = 3000

export type Recording = { kind: 'audio'; wav: Blob } | { kind: 'silence' }

export type MicrophoneError = 'denied' | 'unsupported'

export interface Recorder {
  stop: () => Recording
  level: () => number
  cancel: () => void
}

export const microphoneSupported = (): boolean => typeof navigator.mediaDevices?.getUserMedia === 'function'

export async function startRecording(): Promise<Recorder> {
  const stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true } })
  const context = new AudioContext()
  const source = context.createMediaStreamSource(stream)
  const processor = context.createScriptProcessor(BUFFER_SIZE, 1, 1)
  const chunks: Float32Array[] = []
  let level = 0
  let heard: () => void = () => undefined
  const firstSound = new Promise<void>((resolve) => {
    heard = resolve
    setTimeout(resolve, FIRST_SOUND_TIMEOUT_MS)
  })
  processor.onaudioprocess = (event) => {
    const chunk = new Float32Array(event.inputBuffer.getChannelData(0))
    chunks.push(chunk)
    level = rms(chunk)
    if (level > 0) heard()
  }
  source.connect(processor)
  processor.connect(context.destination)
  await context.resume()
  await firstSound

  const release = () => {
    processor.disconnect()
    source.disconnect()
    stream.getTracks().forEach((track) => track.stop())
    void context.close()
  }

  return {
    stop: () => {
      release()
      const samples = downsample(joinChunks(chunks), context.sampleRate)
      if (isSilent(samples)) return { kind: 'silence' }
      return { kind: 'audio', wav: new Blob([encodeWav(samples)], { type: WAV_MIME_TYPE }) }
    },
    level: () => level,
    cancel: release,
  }
}

export function microphoneErrorOf(error: unknown): MicrophoneError {
  return error instanceof DOMException && (error.name === 'NotAllowedError' || error.name === 'SecurityError') ? 'denied' : 'unsupported'
}
