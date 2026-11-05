import { notifyOutcome } from '../telegram/haptics'
import { prefersLessMotion } from './motion'

const PIECES = 90
const DURATION_MS = 2200
const GRAVITY = 0.35
const COLORS = ['#006666', '#4fa3a3', '#f2b31d', '#e5484d', '#6d28d9']
const COIN_COUNT = 6
const COIN_STAGGER_MS = 90
const COIN_FLIGHT_MS = 700

export function burstConfetti(): void {
  notifyOutcome('success')
  if (prefersLessMotion()) return
  const canvas = document.createElement('canvas')
  canvas.className = 'confetti'
  document.body.appendChild(canvas)
  const width = (canvas.width = window.innerWidth)
  const height = (canvas.height = window.innerHeight)
  const context = canvas.getContext('2d')
  if (!context) return canvas.remove()
  const pieces = Array.from({ length: PIECES }, () => ({
    x: width / 2,
    y: height * 0.6,
    vx: (Math.random() - 0.5) * 10,
    vy: -Math.random() * 12 - 4,
    size: Math.random() * 6 + 4,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
    spin: Math.random() * Math.PI,
  }))
  const started = performance.now()
  const frame = (now: number) => {
    context.clearRect(0, 0, width, height)
    pieces.forEach((piece) => {
      piece.vy += GRAVITY
      piece.x += piece.vx
      piece.y += piece.vy
      piece.spin += 0.1
      context.save()
      context.translate(piece.x, piece.y)
      context.rotate(piece.spin)
      context.fillStyle = piece.color
      context.fillRect(-piece.size / 2, -piece.size / 4, piece.size, piece.size / 2)
      context.restore()
    })
    if (now - started < DURATION_MS) requestAnimationFrame(frame)
    else canvas.remove()
  }
  requestAnimationFrame(frame)
}

export function flyCoins(from: Element, to: Element): void {
  if (prefersLessMotion() || typeof document.body.animate !== 'function') return
  const start = from.getBoundingClientRect()
  const end = to.getBoundingClientRect()
  Array.from({ length: COIN_COUNT }).forEach((_, index) => {
    const coin = document.createElement('span')
    coin.className = 'coin'
    coin.style.left = `${start.left + start.width / 2}px`
    coin.style.top = `${start.top + start.height / 2}px`
    document.body.appendChild(coin)
    coin.animate(
      [{ transform: 'translate(-50%, -50%)' }, { transform: `translate(calc(-50% + ${end.left + end.width / 2 - start.left - start.width / 2}px), calc(-50% + ${end.top + end.height / 2 - start.top - start.height / 2}px)) scale(0.6)` }],
      { duration: COIN_FLIGHT_MS, delay: index * COIN_STAGGER_MS, easing: 'cubic-bezier(0.4, -0.4, 0.6, 1.4)', fill: 'forwards' },
    ).onfinish = () => coin.remove()
  })
}
