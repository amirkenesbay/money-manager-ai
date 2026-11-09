import { reportOpenedFrom } from '../me/meApi'
import { launchSource } from './launchScreen'

let reported = false

export function reportLaunchSource(): void {
  const source = launchSource()
  if (reported || !source) return
  reported = true
  reportOpenedFrom(source).catch(() => undefined)
}
