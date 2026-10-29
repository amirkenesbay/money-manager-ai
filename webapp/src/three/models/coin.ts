import { CylinderGeometry, Group, Mesh, MeshStandardMaterial, TorusGeometry } from 'three'
import type { MomentInputs } from '../moments'
import { approach, type ModelFactory, type Vector } from './model'

const RADIAL_SEGMENTS = 48
const TRAY_TOP = 1.6
const TRAY_BOTTOM = 1.4
const TRAY_HEIGHT = 0.35
const TRAY_Y = -1
const INNER_RADIUS = 1.3
const INNER_HEIGHT = 0.06
const INNER_Y = -0.8
const COIN_RADIUS = 0.62
const COIN_THICKNESS = 0.14
const COIN_RIM_TUBE = 0.035
const COIN_REST = -0.7
const DROP_HEIGHT = 3.2
const GRAVITY = 18
const BOUNCE = 0.42
const SPIN_LOSS = 0.4
const START_SPIN = 8
const BOUNCES = 4
const SETTLE_RATE = 12
const FLATTEN_RATE = 10
const INNER_SPIN = 0.2
const TRAY_ROUGHNESS = 0.85
const INNER_ROUGHNESS = 0.9
const COIN_ROUGHNESS = 0.28
const COIN_METALNESS = 0.55
const CAMERA_FOV = 36
const CAMERA_POSITION: Vector = [0, 1.9, 5]
const CAMERA_TARGET: Vector = [0, -0.5, 0]

type CoinInput = MomentInputs['coin']

function buildCoin(material: MeshStandardMaterial): Group {
  const coin = new Group()
  const body = new Mesh(new CylinderGeometry(COIN_RADIUS, COIN_RADIUS, COIN_THICKNESS, RADIAL_SEGMENTS), material)
  const rimGeometry = new TorusGeometry(COIN_RADIUS, COIN_RIM_TUBE, 12, RADIAL_SEGMENTS)
  const rims = [COIN_THICKNESS / 2, -COIN_THICKNESS / 2].map((y) => {
    const rim = new Mesh(rimGeometry, material)
    rim.rotation.x = Math.PI / 2
    rim.position.y = y
    return rim
  })
  coin.add(body, ...rims)
  return coin
}

export const createCoin: ModelFactory<CoinInput> = (read) => {
  const trayMaterial = new MeshStandardMaterial({ roughness: TRAY_ROUGHNESS })
  const innerMaterial = new MeshStandardMaterial({ roughness: INNER_ROUGHNESS })
  const coinMaterial = new MeshStandardMaterial({ roughness: COIN_ROUGHNESS, metalness: COIN_METALNESS })
  const tray = new Mesh(new CylinderGeometry(TRAY_TOP, TRAY_BOTTOM, TRAY_HEIGHT, RADIAL_SEGMENTS), trayMaterial)
  const inner = new Mesh(new CylinderGeometry(INNER_RADIUS, INNER_RADIUS, INNER_HEIGHT, RADIAL_SEGMENTS), innerMaterial)
  tray.position.y = TRAY_Y
  inner.position.y = INNER_Y
  const coin = buildCoin(coinMaterial)
  coin.position.y = COIN_REST + DROP_HEIGHT
  const root = new Group()
  root.add(tray, inner, coin)
  let velocity = 0
  let spin = START_SPIN
  let bounces = 0

  const fall = (delta: number) => {
    velocity -= GRAVITY * delta
    coin.position.y += velocity * delta
    coin.rotation.x += spin * delta
    if (coin.position.y > COIN_REST) return
    coin.position.y = COIN_REST
    velocity = -velocity * BOUNCE
    spin *= SPIN_LOSS
    bounces += 1
    if (bounces === 1) read().onLanded()
  }

  const settle = (delta: number) => {
    coin.position.y = approach(coin.position.y, COIN_REST, SETTLE_RATE, delta)
    coin.rotation.x = approach(coin.rotation.x, Math.round(coin.rotation.x / Math.PI) * Math.PI, FLATTEN_RATE, delta)
  }

  return {
    root,
    view: { fov: CAMERA_FOV, position: CAMERA_POSITION, target: CAMERA_TARGET },
    theme: (palette) => {
      trayMaterial.color.copy(palette.surface)
      innerMaterial.color.copy(palette.shadow)
      coinMaterial.color.copy(palette.accent)
    },
    update: (time, delta) => {
      if (bounces < BOUNCES) fall(delta)
      else settle(delta)
      inner.rotation.y = time * INNER_SPIN
    },
  }
}
