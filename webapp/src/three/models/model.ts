import { CanvasTexture, Mesh, MeshBasicMaterial, type Object3D, PlaneGeometry, type Raycaster } from 'three'
import type { Palette } from '../palette'

export type Vector = [number, number, number]

export interface CameraView {
  fov: number
  position: Vector
  target: Vector
}

export interface Model {
  root: Object3D
  view: CameraView
  theme: (palette: Palette) => void
  update: (time: number, delta: number) => void
  tap?: (ray: Raycaster) => void
  drag?: (turn: number) => void
}

export type ModelFactory<I> = (read: () => I) => Model

const ORIGIN: Vector = [0, 0, 0]
const DEFAULT_FOV = 40
const SHADOW_TEXTURE_SIZE = 128
const SHADOW_CORE = 4
const SHADOW_OPACITY = 'rgba(0, 0, 0, 0.35)'
const SHADOW_EDGE = 'rgba(0, 0, 0, 0)'
const SHADOW_ASPECT = 0.35
const SHADOW_DEPTH = -0.2

export const frontView = (distance: number, fov: number = DEFAULT_FOV): CameraView => ({ fov, position: [0, 0, distance], target: ORIGIN })

export const approach = (current: number, target: number, rate: number, delta: number): number =>
  current + (target - current) * Math.min(1, delta * rate)

export const clamp = (value: number, min: number, max: number): number => Math.min(max, Math.max(min, value))

export function contactShadow(width: number, y: number): Mesh {
  const canvas = document.createElement('canvas')
  canvas.width = SHADOW_TEXTURE_SIZE
  canvas.height = SHADOW_TEXTURE_SIZE
  const context = canvas.getContext('2d')
  const center = SHADOW_TEXTURE_SIZE / 2
  if (context) {
    const gradient = context.createRadialGradient(center, center, SHADOW_CORE, center, center, center)
    gradient.addColorStop(0, SHADOW_OPACITY)
    gradient.addColorStop(1, SHADOW_EDGE)
    context.fillStyle = gradient
    context.fillRect(0, 0, SHADOW_TEXTURE_SIZE, SHADOW_TEXTURE_SIZE)
  }
  const material = new MeshBasicMaterial({ map: new CanvasTexture(canvas), transparent: true, depthWrite: false })
  const shadow = new Mesh(new PlaneGeometry(width, width * SHADOW_ASPECT), material)
  shadow.position.set(0, y, SHADOW_DEPTH)
  return shadow
}
