import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { periodRange } from '../operations/periods'
import { strings } from '../strings'
import { group, me } from '../test/fixtures'
import { exportOperations } from './exportApi'
import { ExportSheet } from './ExportSheet'

vi.mock('./exportApi', () => ({ exportOperations: vi.fn() }))
vi.mock('../telegram/telegramEnv', () => ({ openExternalLink: vi.fn(), rawInitData: () => undefined }))

const onSent = vi.fn()
const renderSheet = () => render(<ExportSheet me={me()} group={group()} onSent={onSent} onClose={vi.fn()} />)

describe('ExportSheet', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('sends this month as Excel by default', async () => {
    vi.mocked(exportOperations).mockResolvedValue({ count: 42 })
    renderSheet()

    await userEvent.click(screen.getByRole('button', { name: strings.exportSend }))

    expect(exportOperations).toHaveBeenCalledWith(group().id, periodRange('thisMonth', new Date(), 1), 'XLSX')
    expect(onSent).toHaveBeenCalledWith(42)
  })

  it('sends CSV for the chosen period', async () => {
    vi.mocked(exportOperations).mockResolvedValue({ count: 3 })
    renderSheet()

    await userEvent.click(screen.getByRole('radio', { name: strings.period('lastMonth') }))
    await userEvent.click(screen.getByRole('radio', { name: 'CSV' }))
    await userEvent.click(screen.getByRole('button', { name: strings.exportSend }))

    expect(exportOperations).toHaveBeenCalledWith(group().id, periodRange('lastMonth', new Date(), 1), 'CSV')
  })

  it('says when the period is empty', async () => {
    vi.mocked(exportOperations).mockResolvedValue({ count: 0 })
    renderSheet()

    await userEvent.click(screen.getByRole('button', { name: strings.exportSend }))

    expect(await screen.findByText(strings.exportEmpty)).toBeInTheDocument()
    expect(onSent).not.toHaveBeenCalled()
  })

  it('offers Premium for history beyond the free limit', async () => {
    vi.mocked(exportOperations).mockRejectedValue(new ApiRequestError('limit', 422, { code: 'PREMIUM_REQUIRED', message: 'limit', details: { historyDaysBack: 30 } }))
    renderSheet()

    await userEvent.click(screen.getByRole('radio', { name: strings.period('allTime') }))
    await userEvent.click(screen.getByRole('button', { name: strings.exportSend }))

    expect(await screen.findByText(strings.premiumHistoryTitle(30))).toBeInTheDocument()
  })
})
