import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { strings } from '../strings'
import { group } from '../test/fixtures'
import { setGroupNotices } from './groupsApi'
import { NoticesSheet } from './NoticesSheet'

vi.mock('./groupsApi', () => ({ setGroupNotices: vi.fn() }))

describe('NoticesSheet', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('switches the group to important notices only', async () => {
    const family = group({ type: 'SHARED', memberCount: 3 })
    const saved = { ...family, noticeLevel: 'IMPORTANT' as const }
    const onSaved = vi.fn()
    vi.mocked(setGroupNotices).mockResolvedValue(saved)
    render(<NoticesSheet group={family} onSaved={onSaved} onClose={vi.fn()} />)

    await userEvent.click(screen.getByRole('radio', { name: strings.groupNoticeLevels.IMPORTANT }))
    expect(screen.getByText(strings.groupNoticeDetails.IMPORTANT)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.done }))

    expect(setGroupNotices).toHaveBeenCalledWith(family.id, 'IMPORTANT')
    expect(onSaved).toHaveBeenCalledWith(saved)
  })

  it('closes without a request when nothing changed', async () => {
    const onClose = vi.fn()
    render(<NoticesSheet group={group({ type: 'SHARED' })} onSaved={vi.fn()} onClose={onClose} />)

    await userEvent.click(screen.getByRole('button', { name: strings.done }))

    expect(setGroupNotices).not.toHaveBeenCalled()
    expect(onClose).toHaveBeenCalled()
  })
})
