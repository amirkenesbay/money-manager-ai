import { BufferAttribute, BufferGeometry, Group, Mesh, MeshPhysicalMaterial, OctahedronGeometry, Points, PointsMaterial } from 'three'
import type { MomentInputs } from '../moments'
import { approach, contactShadow, frontView, type ModelFactory } from './model'

const WIDTH_SCALE = 0.9
const HEIGHT_SCALE = 1.35
const SPARK_COUNT = 70
const SPARK_MIN_RADIUS = 1.5
const SPARK_RADIUS_SPREAD = 0.9
const SPARK_HEIGHT = 2.4
const SPARK_MIN_SPEED = 0.3
const SPARK_SPEED_SPREAD = 0.7
const SPARK_SIZE = 0.07
const SPARK_DEPTH = 0.6
const SPARK_BOB = 0.1
const SHADOW_WIDTH = 2.2
const SHADOW_Y = -1.75
const CAMERA_DISTANCE = 6
const BOOST_DECAY = 1.8
const IDLE_SPIN = 0.5
const BOOST_SPIN = 9
const BOOST_ORBIT = 3
const BOOST_SPREAD = 0.8
const FLOAT_HEIGHT = 0.12
const FLOAT_SPEED = 1.4
const BASE_OPACITY = 0.45
const PULSE_OPACITY = 0.35
const BOOST_OPACITY = 0.2
const ROUGHNESS = 0.12
const METALNESS = 0.15
const CLEARCOAT_ROUGHNESS = 0.1

interface Spark {
  radius: number
  angle: number
  height: number
  speed: number
}

const randomSpark = (): Spark => ({
  radius: SPARK_MIN_RADIUS + Math.random() * SPARK_RADIUS_SPREAD,
  angle: Math.random() * Math.PI * 2,
  height: (Math.random() - 0.5) * SPARK_HEIGHT,
  speed: SPARK_MIN_SPEED + Math.random() * SPARK_SPEED_SPREAD,
})

export const createCrystal: ModelFactory<MomentInputs['crystal']> = () => {
  const geometry = new OctahedronGeometry(1, 0)
  geometry.scale(WIDTH_SCALE, HEIGHT_SCALE, WIDTH_SCALE)
  const material = new MeshPhysicalMaterial({ roughness: ROUGHNESS, metalness: METALNESS, clearcoat: 1, clearcoatRoughness: CLEARCOAT_ROUGHNESS, flatShading: true })
  const crystal = new Mesh(geometry, material)
  const sparks = Array.from({ length: SPARK_COUNT }, randomSpark)
  const sparkGeometry = new BufferGeometry()
  sparkGeometry.setAttribute('position', new BufferAttribute(new Float32Array(SPARK_COUNT * 3), 3))
  const sparkMaterial = new PointsMaterial({ size: SPARK_SIZE, transparent: true, depthWrite: false })
  const root = new Group()
  root.add(crystal, new Points(sparkGeometry, sparkMaterial), contactShadow(SHADOW_WIDTH, SHADOW_Y))
  let boost = 0

  const orbit = (time: number) => {
    const positions = sparkGeometry.attributes.position.array
    sparks.forEach((spark, index) => {
      const angle = spark.angle + time * spark.speed * (1 + boost * BOOST_ORBIT)
      const radius = spark.radius + boost * BOOST_SPREAD
      positions[index * 3] = Math.cos(angle) * radius
      positions[index * 3 + 1] = spark.height + SPARK_BOB * Math.sin(time * 2 + index)
      positions[index * 3 + 2] = Math.sin(angle) * radius * SPARK_DEPTH
    })
    sparkGeometry.attributes.position.needsUpdate = true
  }

  return {
    root,
    view: frontView(CAMERA_DISTANCE),
    theme: (palette) => {
      material.color.copy(palette.accent)
      sparkMaterial.color.copy(palette.glow)
    },
    update: (time, delta) => {
      boost = approach(boost, 0, BOOST_DECAY, delta)
      crystal.rotation.y += delta * (IDLE_SPIN + boost * BOOST_SPIN)
      crystal.position.y = FLOAT_HEIGHT * Math.sin(time * FLOAT_SPEED)
      orbit(time)
      sparkMaterial.opacity = BASE_OPACITY + PULSE_OPACITY * Math.abs(Math.sin(time * 3)) + boost * BOOST_OPACITY
    },
    tap: () => {
      boost = 1
    },
  }
}
