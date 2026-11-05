import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { category } from '../test/fixtures'
import { ManualFab } from './ManualFab'
import { Sheet } from './Sheet'
import { SwipeRow } from './SwipeRow'

const ROW_WIDTH = 300

const swipe = (element: HTMLElement, distance: number) => {
  fireEvent.pointerDown(element, { clientX: 150, clientY: 10, pointerId: 1 })
  fireEvent.pointerMove(element, { clientX: 150 + distance / 2, clientY: 10, pointerId: 1 })
  fireEvent.pointerMove(element, { clientX: 150 + distance, clientY: 10, pointerId: 1 })
  fireEvent.pointerUp(element, { clientX: 150 + distance, clientY: 10, pointerId: 1 })
}

describe('live components', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(ROW_WIDTH)
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const renderRow = () => {
    const actions = { left: vi.fn(), right: vi.fn(), hold: vi.fn(), click: vi.fn() }
    render(
      <SwipeRow leftLabel="Удалить" rightLabel="Изменить" onSwipeLeft={actions.left} onSwipeRight={actions.right} onLongPress={actions.hold}>
        <button type="button" onClick={actions.click}>Кафе</button>
      </SwipeRow>,
    )
    return { actions, face: screen.getByRole('button', { name: 'Кафе' }).parentElement as HTMLElement }
  }

  it('deletes on a long swipe left and edits on a long swipe right', () => {
    const { actions, face } = renderRow()

    swipe(face, -200)
    swipe(face, 200)

    expect(actions.left).toHaveBeenCalledOnce()
    expect(actions.right).toHaveBeenCalledOnce()
  })

  it('springs back on a short swipe and opens the menu on a long press', () => {
    const { actions, face } = renderRow()

    swipe(face, -60)
    fireEvent.pointerDown(face, { clientX: 150, clientY: 10, pointerId: 1 })
    act(() => vi.advanceTimersByTime(500))
    fireEvent.pointerUp(face, { pointerId: 1 })

    expect(actions.left).not.toHaveBeenCalled()
    expect(actions.hold).toHaveBeenCalledOnce()
  })

  it('closes a sheet dragged down by its handle', () => {
    const onClose = vi.fn()
    const { container } = render(<Sheet label="Язык" onClose={onClose}><p>Русский</p></Sheet>)
    const grip = container.ownerDocument.querySelector('.sheet-grip') as HTMLElement

    fireEvent.pointerDown(grip, { clientY: 10, pointerId: 1 })
    fireEvent.pointerMove(grip, { clientY: 400, pointerId: 1 })
    fireEvent.pointerUp(grip, { clientY: 400, pointerId: 1 })
    act(() => vi.advanceTimersByTime(300))

    expect(onClose).toHaveBeenCalledOnce()
  })

  it('fans out frequent categories when the plus is held', () => {
    const onOpen = vi.fn()
    const coffee = category({ id: 'cat-coffee', name: 'Кофе', icon: '☕' })
    render(<ManualFab quickCategories={[coffee]} onOpen={onOpen} />)
    const plus = screen.getAllByRole('button').at(-1) as HTMLElement

    fireEvent.pointerDown(plus, { clientX: 10, clientY: 10, pointerId: 1 })
    act(() => vi.advanceTimersByTime(500))
    fireEvent.pointerUp(plus, { clientX: 10, clientY: 10, pointerId: 1 })
    fireEvent.click(screen.getByRole('button', { name: 'Кофе' }))

    expect(onOpen).toHaveBeenCalledWith(coffee)
  })
})
