import { Group, Mesh, MeshBasicMaterial, MeshStandardMaterial, SphereGeometry, TorusGeometry } from 'three'
import type { MomentInputs } from '../moments'
import { approach, contactShadow, frontView, type ModelFactory } from './model'

const WIDTH_SEGMENTS = 72
const HEIGHT_SEGMENTS = 48
const HALO_RADIUS = 1.45
const HALO_TUBE = 0.012
const HALO_SEGMENTS = 128
const SHADOW_WIDTH = 2.6
const SHADOW_Y = -1.35
const CAMERA_DISTANCE = 5
const ROUGHNESS = 0.32
const METALNESS = 0.06
const LEVEL_GAIN = 8
const LISTENING_FLOOR = 0.12
const THINKING_AMPLITUDE = 0.05
const LISTENING_SCALE = 1.06
const THINKING_SCALE = 0.72
const LISTENING_SPIN = 0.3
const THINKING_SPIN = 4
const DISPLACEMENT = 0.2
const AMPLITUDE_RATE = 10
const SCALE_RATE = 6
const HALO_GROWTH = 0.18
const HALO_BASE_OPACITY = 0.25
const HALO_LEVEL_OPACITY = 0.35
const HALO_THINKING_OPACITY = 0.18

type OrbInput = MomentInputs['orb']

const ripple = (x: number, y: number, z: number, time: number): number =>
  Math.sin(x * 3.1 + time * 2.4) * Math.sin(y * 2.7 + time * 1.9) * Math.sin(z * 3.3 + time * 2.7)
  + 0.5 * Math.sin(x * 6 + y * 5 + time * 4.1)

export const createOrb: ModelFactory<OrbInput> = (read) => {
  const geometry = new SphereGeometry(1, WIDTH_SEGMENTS, HEIGHT_SEGMENTS)
  const base = geometry.attributes.position.array.slice()
  const material = new MeshStandardMaterial({ roughness: ROUGHNESS, metalness: METALNESS })
  const orb = new Mesh(geometry, material)
  const haloMaterial = new MeshBasicMaterial({ transparent: true })
  const halo = new Mesh(new TorusGeometry(HALO_RADIUS, HALO_TUBE, 8, HALO_SEGMENTS), haloMaterial)
  const shadow = contactShadow(SHADOW_WIDTH, SHADOW_Y)
  const root = new Group()
  root.add(orb, halo, shadow)
  let amplitude = 0
  let scale = 1
  let spin = 0

  const deform = (time: number) => {
    const positions = geometry.attributes.position.array
    for (let index = 0; index < positions.length; index += 3) {
      const x = base[index]
      const y = base[index + 1]
      const z = base[index + 2]
      const radius = 1 + amplitude * DISPLACEMENT * ripple(x, y, z, time)
      positions[index] = x * radius
      positions[index + 1] = y * radius
      positions[index + 2] = z * radius
    }
    geometry.attributes.position.needsUpdate = true
    geometry.computeVertexNormals()
  }

  return {
    root,
    view: frontView(CAMERA_DISTANCE),
    theme: (palette) => {
      material.color.copy(palette.accent)
      haloMaterial.color.copy(palette.accent)
    },
    update: (time, delta) => {
      const { mode, level } = read()
      const listening = mode === 'listening'
      const targetAmplitude = listening ? Math.min(1, LISTENING_FLOOR + level() * LEVEL_GAIN) : THINKING_AMPLITUDE
      amplitude = approach(amplitude, targetAmplitude, AMPLITUDE_RATE, delta)
      scale = approach(scale, listening ? LISTENING_SCALE : THINKING_SCALE, SCALE_RATE, delta)
      spin += delta * (listening ? LISTENING_SPIN : THINKING_SPIN)
      deform(time)
      orb.scale.setScalar(scale)
      orb.rotation.y = spin
      halo.scale.setScalar(1 + amplitude * HALO_GROWTH)
      haloMaterial.opacity = listening ? HALO_BASE_OPACITY + amplitude * HALO_LEVEL_OPACITY : HALO_THINKING_OPACITY
      shadow.scale.setScalar(scale)
    },
  }
}
