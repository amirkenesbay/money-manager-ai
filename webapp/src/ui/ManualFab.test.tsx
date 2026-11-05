import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { strings } from '../strings'
import { ManualFab } from './ManualFab'
import { TabBar } from './TabBar'

describe('ManualFab', () => {
  it('opens the manual form on a tap', () => {
    const onOpen = vi.fn()
    render(<ManualFab onOpen={onOpen} />)
    const button = screen.getByRole('button', { name: strings.manualEntryLabel })

    fireEvent.pointerDown(button, { clientX: 100, clientY: 100, pointerId: 1 })
    fireEvent.pointerUp(button, { clientX: 102, clientY: 101, pointerId: 1 })

    expect(onOpen).toHaveBeenCalledOnce()
  })

  it('moves instead of opening when dragged', () => {
    const onOpen = vi.fn()
    render(<ManualFab onOpen={onOpen} />)
    const button = screen.getByRole('button', { name: strings.manualEntryLabel })

    fireEvent.pointerDown(button, { clientX: 100, clientY: 300, pointerId: 1 })
    fireEvent.pointerMove(button, { clientX: 40, clientY: 200, pointerId: 1 })
    fireEvent.pointerUp(button, { clientX: 40, clientY: 200, pointerId: 1 })

    expect(onOpen).not.toHaveBeenCalled()
    expect(button.closest('.manual-fab')).toHaveClass('left')
  })
})

describe('TabBar', () => {
  it('records voice from the middle of the bar on every tab', () => {
    const onRecordVoice = vi.fn()
    render(<TabBar active="reports" onSelect={vi.fn()} onRecordVoice={onRecordVoice} onShowQuestions={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: strings.recordVoice }))

    expect(onRecordVoice).toHaveBeenCalledOnce()
  })

  it('shows what can be asked on a long press instead of recording', () => {
    vi.useFakeTimers()
    const onRecordVoice = vi.fn()
    const onShowQuestions = vi.fn()
    render(<TabBar active="home" onSelect={vi.fn()} onRecordVoice={onRecordVoice} onShowQuestions={onShowQuestions} />)
    const mic = screen.getByRole('button', { name: strings.recordVoice })

    fireEvent.pointerDown(mic)
    vi.advanceTimersByTime(600)
    fireEvent.pointerUp(mic)
    fireEvent.click(mic)
    vi.useRealTimers()

    expect(onShowQuestions).toHaveBeenCalledOnce()
    expect(onRecordVoice).not.toHaveBeenCalled()
  })
})
