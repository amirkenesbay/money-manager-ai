import { useState } from 'react'
import { notifyOutcome } from '../telegram/haptics'

export function useShake() {
  const [shaking, setShaking] = useState(false)
  return {
    shakeClass: shaking ? 'shake' : '',
    shake: () => {
      notifyOutcome('error')
      setShaking(true)
    },
    onAnimationEnd: () => setShaking(false),
  }
}
