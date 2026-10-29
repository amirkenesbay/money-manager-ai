import { DoubleSide, Group, Mesh, MeshStandardMaterial, PlaneGeometry } from 'three'
import type { MomentInputs } from '../moments'
import { approach, clamp, frontView, type ModelFactory } from './model'

const WIDTH = 16
const HEIGHT = 3.4
const WIDTH_SEGMENTS = 140
const HEIGHT_SEGMENTS = 24
const MIN_LEVEL = 0.08
const MAX_LEVEL = 0.7
const LEVEL_RATE = 2.2
const SPLASH_DECAY = 1.6
const SPLASH_THRESHOLD = 0.01
const CALM_HEIGHT = 0.07
const SPLASH_HEIGHT = 0.25
const RIPPLE_HEIGHT = 0.04
const DEPTH_SWELL = 0.18
const BACK_DEPTH = -0.4
const BACK_PHASE = 1.7
const FRONT_OPACITY = 0.92
const BACK_OPACITY = 0.55
const ROUGHNESS = 0.45
const METALNESS = 0.05
const CAMERA_FOV = 36
const CAMERA_DISTANCE = 5

interface Layer {
  geometry: PlaneGeometry
  base: ArrayLike<number>
  material: MeshStandardMaterial
  phase: number
}

function buildLayer(root: Group, depth: number, phase: number): Layer {
  const geometry = new PlaneGeometry(WIDTH, HEIGHT, WIDTH_SEGMENTS, HEIGHT_SEGMENTS)
  const material = new MeshStandardMaterial({ roughness: ROUGHNESS, metalness: METALNESS, transparent: true, side: DoubleSide })
  const mesh = new Mesh(geometry, material)
  mesh.position.z = depth
  root.add(mesh)
  return { geometry, base: geometry.attributes.position.array.slice(), material, phase }
}

export const createWave: ModelFactory<MomentInputs['wave']> = (read) => {
  const root = new Group()
  const back = buildLayer(root, BACK_DEPTH, BACK_PHASE)
  const front = buildLayer(root, 0, 0)
  const target = () => clamp(read().level, MIN_LEVEL, MAX_LEVEL)
  let level = target()
  let aim = level
  let splash = 0

  const shape = (layer: Layer, time: number) => {
    const positions = layer.geometry.attributes.position.array
    const top = -HEIGHT / 2 + level * HEIGHT
    for (let index = 0; index < positions.length; index += 3) {
      const x = layer.base[index]
      const share = (layer.base[index + 1] + HEIGHT / 2) / HEIGHT
      const crest = (CALM_HEIGHT + splash * SPLASH_HEIGHT) * Math.sin(x * 1.6 + time * 1.2 + layer.phase)
        + RIPPLE_HEIGHT * Math.sin(x * 3.7 - time * 1.7 + layer.phase)
      positions[index + 1] = -HEIGHT / 2 + share * (top + crest + HEIGHT / 2)
      positions[index + 2] = DEPTH_SWELL * share * Math.sin(x * 2.1 + time * 1.3 + layer.phase)
    }
    layer.geometry.attributes.position.needsUpdate = true
    layer.geometry.computeVertexNormals()
  }

  return {
    root,
    view: frontView(CAMERA_DISTANCE, CAMERA_FOV),
    theme: (palette) => {
      front.material.color.copy(palette.accent)
      front.material.opacity = FRONT_OPACITY
      back.material.color.copy(palette.glow)
      back.material.opacity = BACK_OPACITY
    },
    update: (time, delta) => {
      const next = target()
      if (Math.abs(next - aim) > SPLASH_THRESHOLD) splash = 1
      aim = next
      level = approach(level, aim, LEVEL_RATE, delta)
      splash = approach(splash, 0, SPLASH_DECAY, delta)
      shape(back, time)
      shape(front, time)
    },
  }
}
