export const TARGET_SAMPLE_RATE = 16_000
export const WAV_MIME_TYPE = 'audio/wav'

const SILENCE_RMS = 0.01
const HEADER_SIZE = 44
const BYTES_PER_SAMPLE = 2
const MAX_SAMPLE = 0x7fff
const MIN_SAMPLE = -0x8000
const PCM_FORMAT = 1
const MONO = 1
const BITS_PER_SAMPLE = 16
const FMT_CHUNK_SIZE = 16

export function downsample(samples: Float32Array, fromRate: number, toRate: number = TARGET_SAMPLE_RATE): Float32Array {
  if (fromRate <= toRate) return samples
  const ratio = fromRate / toRate
  const result = new Float32Array(Math.floor(samples.length / ratio))
  for (let index = 0; index < result.length; index += 1) {
    const start = Math.floor(index * ratio)
    const end = Math.min(Math.floor((index + 1) * ratio), samples.length)
    let sum = 0
    for (let position = start; position < end; position += 1) sum += samples[position]
    result[index] = sum / Math.max(end - start, 1)
  }
  return result
}

export function rms(samples: Float32Array): number {
  if (samples.length === 0) return 0
  let sum = 0
  for (const sample of samples) sum += sample * sample
  return Math.sqrt(sum / samples.length)
}

export const isSilent = (samples: Float32Array): boolean => rms(samples) < SILENCE_RMS

export function encodeWav(samples: Float32Array, sampleRate: number = TARGET_SAMPLE_RATE): ArrayBuffer {
  const dataSize = samples.length * BYTES_PER_SAMPLE
  const buffer = new ArrayBuffer(HEADER_SIZE + dataSize)
  const view = new DataView(buffer)
  const text = (offset: number, value: string) => [...value].forEach((char, index) => view.setUint8(offset + index, char.charCodeAt(0)))
  text(0, 'RIFF')
  view.setUint32(4, HEADER_SIZE - 8 + dataSize, true)
  text(8, 'WAVE')
  text(12, 'fmt ')
  view.setUint32(16, FMT_CHUNK_SIZE, true)
  view.setUint16(20, PCM_FORMAT, true)
  view.setUint16(22, MONO, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * BYTES_PER_SAMPLE, true)
  view.setUint16(32, BYTES_PER_SAMPLE, true)
  view.setUint16(34, BITS_PER_SAMPLE, true)
  text(36, 'data')
  view.setUint32(40, dataSize, true)
  samples.forEach((sample, index) => {
    const clamped = Math.max(-1, Math.min(1, sample))
    view.setInt16(HEADER_SIZE + index * BYTES_PER_SAMPLE, clamped < 0 ? clamped * -MIN_SAMPLE : clamped * MAX_SAMPLE, true)
  })
  return buffer
}

export function joinChunks(chunks: Float32Array[]): Float32Array {
  const joined = new Float32Array(chunks.reduce((length, chunk) => length + chunk.length, 0))
  let offset = 0
  for (const chunk of chunks) {
    joined.set(chunk, offset)
    offset += chunk.length
  }
  return joined
}
