import { describe, expect, it } from 'vitest'
import { downsample, encodeWav, isSilent, TARGET_SAMPLE_RATE } from './wav'

const ascii = (buffer: ArrayBuffer, offset: number, length: number) =>
  String.fromCharCode(...new Uint8Array(buffer, offset, length))

describe('wav', () => {
  it('writes a 16-bit mono header the server accepts', () => {
    const buffer = encodeWav(new Float32Array([0, 0.5, -0.5, 1]))
    const view = new DataView(buffer)

    expect(ascii(buffer, 0, 4)).toBe('RIFF')
    expect(ascii(buffer, 8, 4)).toBe('WAVE')
    expect(view.getUint16(22, true)).toBe(1)
    expect(view.getUint32(24, true)).toBe(TARGET_SAMPLE_RATE)
    expect(view.getUint32(40, true)).toBe(8)
    expect(view.getInt16(50, true)).toBe(0x7fff)
  })

  it('downsamples to 16 kHz by averaging', () => {
    expect([...downsample(new Float32Array([1, 3, 5, 7, 9, 11]), 48_000)]).toEqual([3, 9])
  })

  it('treats a quiet recording as silence', () => {
    expect(isSilent(new Float32Array(1000).fill(0.001))).toBe(true)
    expect(isSilent(new Float32Array(1000).fill(0.2))).toBe(false)
  })
})
