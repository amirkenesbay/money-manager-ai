import { execFileSync } from 'node:child_process'
import { mkdtempSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const RUSSIAN_VOICE = 'Milena'
const WAV_FORMAT = ['-f', 'WAVE', '-d', 'LEI16@16000', '-c', '1']

export function spokenWav(phrase: string): Buffer {
  const directory = mkdtempSync(join(tmpdir(), 'mm-speech-'))
  const aiff = join(directory, 'speech.aiff')
  const wav = join(directory, 'speech.wav')
  execFileSync('say', ['-v', RUSSIAN_VOICE, '-o', aiff, phrase])
  execFileSync('afconvert', [...WAV_FORMAT, aiff, wav])
  return readFileSync(wav)
}

export function fakeMicrophone(wavBase64: string): void {
  const START_DELAY_SECONDS = 0.5
  const bytes = Uint8Array.from(atob(wavBase64), (char) => char.charCodeAt(0))
  navigator.mediaDevices.getUserMedia = async () => {
    const context = new AudioContext()
    await context.resume()
    const buffer = await context.decodeAudioData(bytes.buffer.slice(0))
    const source = context.createBufferSource()
    source.buffer = buffer
    const destination = context.createMediaStreamDestination()
    source.connect(destination)
    source.start(context.currentTime + START_DELAY_SECONDS)
    return destination.stream
  }
}
