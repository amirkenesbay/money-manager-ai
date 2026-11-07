import { useEffect } from 'react'
import type { Me } from '../api/types'
import { updateMe } from './meApi'
import { deviceTimezone } from './timezone'

export function syncDeviceTimezone(me: Me): Promise<Me> | null {
  const timezone = deviceTimezone()
  return me.timezone === timezone ? null : updateMe({ timezone })
}

export function useDeviceTimezone(me: Me, onMeChanged: (me: Me) => void): void {
  useEffect(() => {
    syncDeviceTimezone(me)?.then(onMeChanged).catch(() => undefined)
  }, [me, onMeChanged])
}
