import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDigestMuted } from '../me/meApi'
import { strings } from '../strings'
import { me } from '../test/fixtures'
import { DigestsSheet } from './DigestsSheet'

vi.mock('../me/meApi', () => ({ setDigestMuted: vi.fn() }))

const onChanged = vi.fn()
const onFailed = vi.fn()
const renderSheet = (muted: ('DAY' | 'WEEK' | 'MONTH')[] = []) =>
  render(<DigestsSheet me={me({ mutedDigests: muted })} onChanged={onChanged} onFailed={onFailed} onClose={vi.fn()} />)

describe('DigestsSheet', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows every summary switched on by default with its schedule', () => {
    renderSheet()

    expect(screen.getAllByRole('switch').map((toggle) => toggle.getAttribute('aria-checked'))).toEqual(['true', 'true', 'true'])
    expect(screen.getByText(strings.digestSchedules.WEEK)).toBeInTheDocument()
  })

  it('mutes a summary and passes the updated profile on', async () => {
    const updated = me({ mutedDigests: ['DAY'] })
    vi.mocked(setDigestMuted).mockResolvedValue(updated)
    renderSheet()

    await userEvent.click(screen.getByRole('switch', { name: strings.digestLabels.DAY }))

    expect(setDigestMuted).toHaveBeenCalledWith('DAY', true)
    expect(onChanged).toHaveBeenCalledWith(updated)
  })

  it('turns a muted summary back on and reports a failure', async () => {
    vi.mocked(setDigestMuted).mockRejectedValue(new Error('offline'))
    renderSheet(['MONTH'])

    await userEvent.click(screen.getByRole('switch', { name: strings.digestLabels.MONTH }))

    expect(setDigestMuted).toHaveBeenCalledWith('MONTH', false)
    expect(onFailed).toHaveBeenCalled()
  })
})
