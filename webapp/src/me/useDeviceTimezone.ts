import { useEffect } from 'react'
import type { Me } from '../api/types'
import { updateMe } from './meApi'

const deviceTimezone = (): string => Intl.DateTimeFormat().resolvedOptions().timeZone

export function useDeviceTimezone(me: Me, onMeChanged: (me: Me) => void): void {
  useEffect(() => {
    if (me.timezone !== null) return
    updateMe({ timezone: deviceTimezone() }).then(onMeChanged).catch(() => undefined)
  }, [me.timezone, onMeChanged])
}
