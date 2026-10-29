import { BufferAttribute, BufferGeometry, CanvasTexture, Group, LineBasicMaterial, LineSegments, Mesh, MeshStandardMaterial, SphereGeometry, Sprite, SpriteMaterial, Vector3 } from 'three'
import type { MomentInputs } from '../moments'
import type { Palette } from '../palette'
import { approach, frontView, type ModelFactory } from './model'

const LETTER_TEXTURE_SIZE = 128
const LETTER_FONT_SHARE = 0.56
const LETTER_BASELINE_SHIFT = 6
const LETTER_FONT_WEIGHT = 700
const LETTER_SCALE = 1.1
const LETTER_DEPTH = 1.05
const CIRCLE_RADIUS = 1.25
const CIRCLE_SQUASH = 0.62
const LARGE_BALL = 0.55
const SMALL_BALL = 0.42
const CROWDED = 3
const SPHERE_SEGMENTS = 40
const ARRIVAL: [number, number, number] = [4.5, 1.8, 0]
const SPRING = 30
const DAMPING = 7
const BOB_HEIGHT = 0.06
const BOB_SPEED = 1.6
const LINK_DEPTH = -0.1
const LINK_OPACITY = 0.6
const LINK_REVEAL_DISTANCE = 0.6
const LINK_RATE = 3
const CAMERA_DISTANCE = 6
const OTHERS_ROUGHNESS = 0.75
const YOU_ROUGHNESS = 0.45

type GroupInput = MomentInputs['group']

interface Member {
  body: Group
  home: Vector3
  velocity: Vector3
  letter: HTMLCanvasElement
  texture: CanvasTexture
}

function homeOf(index: number, count: number): Vector3 {
  if (count === 1) return new Vector3()
  const angle = Math.PI / 2 + (index / count) * Math.PI * 2
  return new Vector3(Math.cos(angle) * CIRCLE_RADIUS, Math.sin(angle) * CIRCLE_RADIUS * CIRCLE_SQUASH, 0)
}

function drawLetter(canvas: HTMLCanvasElement, initial: string, palette: Palette): void {
  const context = canvas.getContext('2d')
  if (!context) return
  const center = LETTER_TEXTURE_SIZE / 2
  context.clearRect(0, 0, LETTER_TEXTURE_SIZE, LETTER_TEXTURE_SIZE)
  context.font = `${LETTER_FONT_WEIGHT} ${LETTER_TEXTURE_SIZE * LETTER_FONT_SHARE}px ${palette.monoFont}`
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.fillStyle = palette.surfaceCss
  context.fillText(initial, center, center + LETTER_BASELINE_SHIFT)
}

function pairsOf(count: number): [number, number][] {
  const pairs: [number, number][] = []
  for (let from = 0; from < count; from += 1) {
    for (let to = from + 1; to < count; to += 1) pairs.push([from, to])
  }
  return pairs
}

export const createGroup: ModelFactory<GroupInput> = (read) => {
  const { initials, you } = read()
  const count = initials.length
  const ballRadius = count > CROWDED ? SMALL_BALL : LARGE_BALL
  const othersMaterial = new MeshStandardMaterial({ roughness: OTHERS_ROUGHNESS })
  const youMaterial = new MeshStandardMaterial({ roughness: YOU_ROUGHNESS })
  const sphere = new SphereGeometry(ballRadius, SPHERE_SEGMENTS, SPHERE_SEGMENTS)
  const root = new Group()
  const members: Member[] = initials.map((_, index) => {
    const letter = document.createElement('canvas')
    letter.width = LETTER_TEXTURE_SIZE
    letter.height = LETTER_TEXTURE_SIZE
    const texture = new CanvasTexture(letter)
    const sprite = new Sprite(new SpriteMaterial({ map: texture, transparent: true, depthTest: false }))
    sprite.scale.setScalar(ballRadius * LETTER_SCALE)
    sprite.position.z = ballRadius * LETTER_DEPTH
    const body = new Group()
    body.add(new Mesh(sphere, index === you ? youMaterial : othersMaterial), sprite)
    const home = homeOf(index, count)
    body.position.copy(index === you ? new Vector3(...ARRIVAL) : home)
    root.add(body)
    return { body, home, velocity: new Vector3(), letter, texture }
  })
  const pairs = pairsOf(count)
  const linkGeometry = new BufferGeometry()
  linkGeometry.setAttribute('position', new BufferAttribute(new Float32Array(pairs.length * 6), 3))
  const linkMaterial = new LineBasicMaterial({ transparent: true, opacity: 0 })
  root.add(new LineSegments(linkGeometry, linkMaterial))
  const arriving = members[you]

  const link = () => {
    const positions = linkGeometry.attributes.position.array
    pairs.forEach(([from, to], index) => {
      const a = members[from].body.position
      const b = members[to].body.position
      positions.set([a.x, a.y, a.z + LINK_DEPTH, b.x, b.y, b.z + LINK_DEPTH], index * 6)
    })
    linkGeometry.attributes.position.needsUpdate = true
  }

  return {
    root,
    view: frontView(CAMERA_DISTANCE),
    theme: (palette) => {
      othersMaterial.color.copy(palette.muted)
      youMaterial.color.copy(palette.accent)
      linkMaterial.color.copy(palette.accent)
      members.forEach((member, index) => {
        drawLetter(member.letter, initials[index], palette)
        member.texture.needsUpdate = true
      })
    },
    update: (time, delta) => {
      members.forEach((member, index) => {
        const target = member.home.clone()
        target.y += BOB_HEIGHT * Math.sin(time * BOB_SPEED + index * 2)
        const force = target.sub(member.body.position).multiplyScalar(SPRING).sub(member.velocity.clone().multiplyScalar(DAMPING))
        member.velocity.addScaledVector(force, delta)
        member.body.position.addScaledVector(member.velocity, delta)
      })
      link()
      const arrived = !arriving || arriving.body.position.distanceTo(arriving.home) < LINK_REVEAL_DISTANCE
      linkMaterial.opacity = approach(linkMaterial.opacity, arrived ? LINK_OPACITY : 0, LINK_RATE, delta)
    },
  }
}
