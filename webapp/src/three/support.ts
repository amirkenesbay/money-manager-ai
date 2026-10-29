const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'
const LOW_PERFORMANCE_AGENT = /Telegram-Android\/.*;\s*LOW\)$/
const MIN_CORES = 3

export function supports3d(): boolean {
  if (typeof WebGLRenderingContext === 'undefined') return false
  if (window.matchMedia?.(REDUCED_MOTION_QUERY).matches) return false
  if (LOW_PERFORMANCE_AGENT.test(navigator.userAgent)) return false
  return (navigator.hardwareConcurrency ?? MIN_CORES) >= MIN_CORES
}
