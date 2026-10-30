import { flushSync } from 'react-dom'

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'
const LOW_PERFORMANCE_AGENT = /Telegram-Android\/.*;\s*LOW\)$/

export type Motion = 'forward' | 'back' | 'fade' | 'dismiss'

export function prefersLessMotion(): boolean {
  if (window.matchMedia?.(REDUCED_MOTION_QUERY).matches) return true
  return LOW_PERFORMANCE_AGENT.test(navigator.userAgent)
}

export function withMotion(motion: Motion, update: () => void): void {
  if (!document.startViewTransition || prefersLessMotion()) {
    update()
    return
  }
  const root = document.documentElement
  root.dataset.motion = motion
  const transition = document.startViewTransition(() => flushSync(update))
  transition.ready.catch(() => undefined)
  void transition.finished.finally(() => {
    if (root.dataset.motion === motion) delete root.dataset.motion
  })
}
