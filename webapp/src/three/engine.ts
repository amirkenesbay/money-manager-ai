import {
  DirectionalLight,
  HemisphereLight,
  type Material,
  type Object3D,
  PerspectiveCamera,
  Raycaster,
  Scene,
  Sprite,
  Texture,
  Vector2,
  WebGLRenderer,
} from 'three'
import { THEME_ATTRIBUTE } from '../theme/theme'
import { createModel } from './models'
import type { CameraView, Model } from './models/model'
import type { MomentInputs, MomentKind } from './moments'
import { type Palette, readPalette } from './palette'

const MAX_PIXEL_RATIO = 2
const MAX_DELTA_SECONDS = 0.05
const MS_IN_SECOND = 1000
const TAP_SLOP_PX = 6
const NEAR = 0.1
const FAR = 100
const LIGHT_SCALE = Math.PI
const KEY_INTENSITY = 0.85
const RIM_INTENSITY = 0.25
const KEY_POSITION = [-3, 4, 5] as const
const RIM_POSITION = [4, -2, 3] as const
const LIGHT_SKY = 0xffffff
const LIGHT_GROUND = 0x9a9896
const LIGHT_INTENSITY = 0.8
const DARK_SKY = 0xcfd6dc
const DARK_GROUND = 0x15171a
const DARK_INTENSITY = 0.6
const CONTEXT_2D = '2d'

interface Stage {
  renderer: WebGLRenderer
  scene: Scene
  camera: PerspectiveCamera
  hemisphere: HemisphereLight
  raycaster: Raycaster
  width: number
  height: number
}

interface View {
  canvas: HTMLCanvasElement
  context: CanvasRenderingContext2D
  model: Model
  visible: boolean
}

const views = new Set<View>()
let stage: Stage | null = null
let frameHandle = 0
let lastFrame = 0
let elapsed = 0

export function attach<K extends MomentKind>(canvas: HTMLCanvasElement, kind: K, read: () => MomentInputs[K]): () => void {
  const context = canvas.getContext(CONTEXT_2D)
  if (!context) throw new Error(`${CONTEXT_2D} canvas context is unavailable`)
  const current = stage ?? (stage = createStage())
  const model = createModel(kind, read)
  model.theme(readPalette())
  const view: View = { canvas, context, model, visible: false }
  const resize = new ResizeObserver(() => fitCanvas(canvas))
  const intersection = new IntersectionObserver(([entry]) => {
    view.visible = entry.isIntersecting
    schedule()
  })
  resize.observe(canvas)
  intersection.observe(canvas)
  const releasePointer = listenPointer(view, current)
  views.add(view)
  fitCanvas(canvas)
  return () => {
    views.delete(view)
    resize.disconnect()
    intersection.disconnect()
    releasePointer()
    disposeTree(model.root)
  }
}

function createStage(): Stage {
  const renderer = new WebGLRenderer({ antialias: true, alpha: true })
  renderer.setPixelRatio(1)
  renderer.setClearColor(0x000000, 0)
  renderer.setScissorTest(true)
  const scene = new Scene()
  const hemisphere = new HemisphereLight()
  const key = new DirectionalLight(0xffffff, KEY_INTENSITY * LIGHT_SCALE)
  key.position.set(...KEY_POSITION)
  const rim = new DirectionalLight(0xffffff, RIM_INTENSITY * LIGHT_SCALE)
  rim.position.set(...RIM_POSITION)
  scene.add(hemisphere, key, rim)
  const created: Stage = { renderer, scene, camera: new PerspectiveCamera(), hemisphere, raycaster: new Raycaster(), width: 0, height: 0 }
  light(created, readPalette())
  document.addEventListener('visibilitychange', schedule)
  new MutationObserver(retheme).observe(document.documentElement, { attributes: true, attributeFilter: [THEME_ATTRIBUTE] })
  return created
}

function light(current: Stage, palette: Palette): void {
  current.hemisphere.color.set(palette.dark ? DARK_SKY : LIGHT_SKY)
  current.hemisphere.groundColor.set(palette.dark ? DARK_GROUND : LIGHT_GROUND)
  current.hemisphere.intensity = (palette.dark ? DARK_INTENSITY : LIGHT_INTENSITY) * LIGHT_SCALE
}

function retheme(): void {
  const palette = readPalette()
  if (stage) light(stage, palette)
  views.forEach((view) => view.model.theme(palette))
}

function fitCanvas(canvas: HTMLCanvasElement): void {
  const ratio = Math.min(window.devicePixelRatio || 1, MAX_PIXEL_RATIO)
  canvas.width = Math.round(canvas.clientWidth * ratio)
  canvas.height = Math.round(canvas.clientHeight * ratio)
}

const isShown = (view: View): boolean => view.visible && view.canvas.width > 0 && view.canvas.height > 0

function schedule(): void {
  if (document.hidden) {
    cancelAnimationFrame(frameHandle)
    frameHandle = 0
    return
  }
  if (frameHandle !== 0 || ![...views].some(isShown)) return
  lastFrame = performance.now()
  frameHandle = requestAnimationFrame(frame)
}

function frame(now: number): void {
  frameHandle = 0
  const current = stage
  const shown = [...views].filter(isShown)
  if (!current || shown.length === 0) return
  const delta = Math.min(MAX_DELTA_SECONDS, (now - lastFrame) / MS_IN_SECOND)
  lastFrame = now
  elapsed += delta
  fitStage(current, shown)
  shown.forEach((view) => draw(current, view, delta))
  frameHandle = requestAnimationFrame(frame)
}

function fitStage(current: Stage, shown: View[]): void {
  const width = Math.max(current.width, ...shown.map((view) => view.canvas.width))
  const height = Math.max(current.height, ...shown.map((view) => view.canvas.height))
  if (width === current.width && height === current.height) return
  current.renderer.setSize(width, height, false)
  current.width = width
  current.height = height
}

function aim(camera: PerspectiveCamera, view: CameraView, aspect: number): void {
  camera.fov = view.fov
  camera.aspect = aspect
  camera.near = NEAR
  camera.far = FAR
  camera.position.set(...view.position)
  camera.lookAt(...view.target)
  camera.updateProjectionMatrix()
}

function draw(current: Stage, view: View, delta: number): void {
  const { canvas, context, model } = view
  const { width, height } = canvas
  model.update(elapsed, delta)
  aim(current.camera, model.view, width / height)
  current.renderer.setViewport(0, 0, width, height)
  current.renderer.setScissor(0, 0, width, height)
  current.scene.add(model.root)
  current.renderer.render(current.scene, current.camera)
  current.scene.remove(model.root)
  context.clearRect(0, 0, width, height)
  context.drawImage(current.renderer.domElement, 0, current.height - height, width, height, 0, 0, width, height)
}

function listenPointer(view: View, current: Stage): () => void {
  const { canvas, model } = view
  const { tap, drag } = model
  if (!tap && !drag) return () => undefined
  let gesture: { last: number; moved: number } | null = null

  const down = (event: PointerEvent) => {
    gesture = { last: event.clientX, moved: 0 }
  }
  const move = (event: PointerEvent) => {
    if (!gesture) return
    const shift = event.clientX - gesture.last
    gesture.last = event.clientX
    gesture.moved += Math.abs(shift)
    drag?.(shift / Math.max(canvas.clientWidth, 1))
  }
  const up = (event: PointerEvent) => {
    if (gesture && gesture.moved < TAP_SLOP_PX && tap) tap(rayAt(current, view, event))
    gesture = null
  }
  const cancel = () => {
    gesture = null
  }
  canvas.addEventListener('pointerdown', down)
  canvas.addEventListener('pointermove', move)
  canvas.addEventListener('pointerup', up)
  canvas.addEventListener('pointercancel', cancel)
  return () => {
    canvas.removeEventListener('pointerdown', down)
    canvas.removeEventListener('pointermove', move)
    canvas.removeEventListener('pointerup', up)
    canvas.removeEventListener('pointercancel', cancel)
  }
}

function rayAt(current: Stage, view: View, event: PointerEvent): Raycaster {
  const bounds = view.canvas.getBoundingClientRect()
  const point = new Vector2(((event.clientX - bounds.left) / bounds.width) * 2 - 1, -((event.clientY - bounds.top) / bounds.height) * 2 + 1)
  aim(current.camera, view.model.view, bounds.width / bounds.height)
  view.model.root.updateMatrixWorld(true)
  current.raycaster.setFromCamera(point, current.camera)
  return current.raycaster
}

function disposeTree(root: Object3D): void {
  root.traverse((node) => {
    const { geometry, material } = node as Object3D & { geometry?: { dispose: () => void }; material?: Material | Material[] }
    if (geometry && !(node instanceof Sprite)) geometry.dispose()
    if (material) [material].flat().forEach(disposeMaterial)
  })
}

function disposeMaterial(material: Material): void {
  Object.values(material).forEach((value) => {
    if (value instanceof Texture) value.dispose()
  })
  material.dispose()
}
