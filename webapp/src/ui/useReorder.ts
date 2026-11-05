import { type PointerEvent, useState } from 'react'
import { holdImpact, selectionTick } from '../telegram/haptics'

const ITEM_SELECTOR = '[data-reorder-id]'

export function useReorder(ids: string[], onDrop: (ids: string[]) => void) {
  const [order, setOrder] = useState<string[] | null>(null)
  const [base, setBase] = useState('')
  const [dragging, setDragging] = useState<string | null>(null)
  const current = order && base === ids.join() ? order : ids

  const handle = (id: string) => ({
    onPointerDown: (event: PointerEvent<HTMLElement>) => {
      event.preventDefault()
      event.currentTarget.setPointerCapture?.(event.pointerId)
      holdImpact()
      setDragging(id)
      setBase(ids.join())
      setOrder(ids)
    },
    onPointerMove: (event: PointerEvent<HTMLElement>) => {
      const list = event.currentTarget.closest('ul')
      if (dragging !== id || !list) return
      const items = Array.from(list.querySelectorAll<HTMLElement>(ITEM_SELECTOR))
      const over = items.find((item) => {
        const box = item.getBoundingClientRect()
        return event.clientY >= box.top && event.clientY <= box.bottom
      })
      const target = over?.dataset.reorderId
      if (!target || target === id) return
      setOrder((previous) => {
        const next = (previous ?? ids).filter((candidate) => candidate !== id)
        next.splice(next.indexOf(target) + (current.indexOf(target) > current.indexOf(id) ? 1 : 0), 0, id)
        return next
      })
      selectionTick()
    },
    onPointerUp: () => {
      if (dragging !== id) return
      setDragging(null)
      if (order && order.join() !== ids.join()) onDrop(order)
    },
  })

  return { order: current, dragging, handle }
}
