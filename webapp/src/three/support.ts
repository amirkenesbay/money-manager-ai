import { prefersLessMotion } from '../ui/motion'

const MIN_CORES = 3

export function supports3d(): boolean {
  if (typeof WebGLRenderingContext === 'undefined') return false
  if (prefersLessMotion()) return false
  return (navigator.hardwareConcurrency ?? MIN_CORES) >= MIN_CORES
}
