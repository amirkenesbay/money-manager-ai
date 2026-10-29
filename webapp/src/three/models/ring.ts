import { Group, Mesh, MeshStandardMaterial, TorusGeometry } from 'three'
import type { MomentInputs } from '../moments'
import { approach, frontView, type ModelFactory } from './model'

const RADIUS = 1.2
const TUBE = 0.34
const TRACK_TUBE = 0.3
const RADIAL_SEGMENTS = 28
const TUBULAR_SEGMENTS = 96
const DEPTH_SCALE = 0.55
const SLICE_GAP = 0.05
const FULL_TURN = Math.PI * 2
const TILT = -0.95
const LIFT = -0.35
const CAMERA_DISTANCE = 5.2
const ROUGHNESS = 0.5
const METALNESS = 0.05
const DRAG_GAIN = 4
const FLING_GAIN = 60
const IDLE_SPEED = 0.12
const FLING_DECAY = 1.5
const SELECTED_OFFSET = 0.28
const SELECTED_LIFT = 0.12
const OFFSET_RATE = 10

type RingInput = MomentInputs['ring']

interface Slice {
  mesh: Mesh
  material: MeshStandardMaterial
  middle: number
  offset: number
}

function buildSlices(values: number[], spinner: Group): Slice[] {
  const total = values.reduce((sum, value) => sum + value, 0)
  if (total <= 0) return []
  let start = 0
  return values.map((value, index) => {
    const share = (value / total) * FULL_TURN
    const material = new MeshStandardMaterial({ roughness: ROUGHNESS, metalness: METALNESS })
    const mesh = new Mesh(new TorusGeometry(RADIUS, TUBE, RADIAL_SEGMENTS, TUBULAR_SEGMENTS, Math.max(share - SLICE_GAP, SLICE_GAP)), material)
    mesh.scale.z = DEPTH_SCALE
    mesh.userData.index = index
    const pivot = new Group()
    pivot.rotation.z = start
    pivot.add(mesh)
    spinner.add(pivot)
    start += share
    return { mesh, material, middle: share / 2, offset: 0 }
  })
}

export const createRing: ModelFactory<RingInput> = (read) => {
  const tilt = new Group()
  tilt.rotation.x = TILT
  tilt.position.y = LIFT
  const spinner = new Group()
  tilt.add(spinner)
  const trackMaterial = new MeshStandardMaterial({ roughness: ROUGHNESS })
  const track = new Mesh(new TorusGeometry(RADIUS, TRACK_TUBE, RADIAL_SEGMENTS, TUBULAR_SEGMENTS), trackMaterial)
  track.scale.z = DEPTH_SCALE
  const slices = buildSlices(read().values, spinner)
  if (slices.length === 0) spinner.add(track)
  let velocity = IDLE_SPEED

  return {
    root: tilt,
    view: frontView(CAMERA_DISTANCE),
    theme: (palette) => {
      trackMaterial.color.copy(palette.line)
      slices.forEach((slice, index) => slice.material.color.copy(palette.charts[index % palette.charts.length]))
    },
    update: (_, delta) => {
      spinner.rotation.z += velocity * delta
      velocity = approach(velocity, IDLE_SPEED, FLING_DECAY, delta)
      const { selected } = read()
      slices.forEach((slice, index) => {
        const chosen = index === selected
        slice.offset = approach(slice.offset, chosen ? SELECTED_OFFSET : 0, OFFSET_RATE, delta)
        slice.mesh.position.set(Math.cos(slice.middle) * slice.offset, Math.sin(slice.middle) * slice.offset, chosen ? SELECTED_LIFT : 0)
      })
    },
    drag: (turn) => {
      spinner.rotation.z -= turn * DRAG_GAIN
      velocity = -turn * FLING_GAIN
    },
    tap: (ray) => {
      const hit = ray.intersectObjects(slices.map((slice) => slice.mesh))[0]
      if (hit) read().onSelect(hit.object.userData.index as number)
    },
  }
}
