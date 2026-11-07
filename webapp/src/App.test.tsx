import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from './api/client'
import { App } from './App'
import { fetchBalance, fetchMonthSummary, fetchRecentOperations } from './dashboard/dashboardApi'
import { fetchGroups } from './groups/groupsApi'
import { fetchMe } from './me/meApi'
import { applyLanguage, dictionaries, strings } from './strings'
import { balance, group, me, operationsPage, summary } from './test/fixtures'

vi.mock('./me/meApi', () => ({ fetchMe: vi.fn(), updateMe: vi.fn(() => new Promise(() => undefined)) }))
vi.mock('./groups/groupsApi', () => ({ fetchGroups: vi.fn(), activateGroup: vi.fn() }))
vi.mock('./dashboard/dashboardApi', () => ({
  RECENT_OPERATIONS_LIMIT: 10,
  fetchBalance: vi.fn(),
  fetchMonthSummary: vi.fn(),
  fetchRecentOperations: vi.fn(),
}))

const fetchMeMock = vi.mocked(fetchMe)

describe('App', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchGroups).mockResolvedValue([group()])
    vi.mocked(fetchBalance).mockResolvedValue(balance())
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary())
    vi.mocked(fetchRecentOperations).mockResolvedValue(operationsPage([]))
  })

  it('asks to open the app from Telegram and does not call the API outside it', () => {
    render(<App insideTelegram={false} />)

    expect(screen.getByText(strings.outsideTelegramTitle)).toBeInTheDocument()
    expect(fetchMeMock).not.toHaveBeenCalled()
  })

  it('shows loading, then the dashboard of the active group', async () => {
    fetchMeMock.mockResolvedValue(me())

    render(<App insideTelegram />)

    expect(screen.getByLabelText(strings.loading)).toBeInTheDocument()
    expect(await screen.findByText(strings.balanceTitle)).toBeInTheDocument()
  })

  it('sends the user to the bot when there is no group yet', async () => {
    fetchMeMock.mockResolvedValue(me({ activeGroupId: null, firstName: null }))

    render(<App insideTelegram />)

    expect(await screen.findByText(strings.noGroupTitle)).toBeInTheDocument()
    expect(screen.getByText(strings.noGroupHint)).toBeInTheDocument()
    expect(fetchBalance).not.toHaveBeenCalled()
  })

  it('shows the server message and recovers on retry', async () => {
    const serverMessage = 'Не удалось подтвердить вход через Telegram.'
    fetchMeMock
      .mockRejectedValueOnce(new ApiRequestError(serverMessage, 401, { code: 'UNAUTHORIZED', message: serverMessage, details: {} }))
      .mockResolvedValueOnce(me())

    render(<App insideTelegram />)

    expect(await screen.findByText(serverMessage)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.retry }))

    expect(await screen.findByText(strings.balanceTitle)).toBeInTheDocument()
  })

  it('tells a blocked user that access is closed and offers no retry', async () => {
    const serverMessage = 'Аккаунт заблокирован.'
    fetchMeMock.mockRejectedValue(new ApiRequestError(serverMessage, 403, { code: 'USER_BLOCKED', message: serverMessage, details: {} }))

    render(<App insideTelegram />)

    expect(await screen.findByText(strings.blockedTitle)).toBeInTheDocument()
    expect(screen.getByText(serverMessage)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: strings.retry })).not.toBeInTheDocument()
  })

  it('shows a generic hint when the network is down', async () => {
    fetchMeMock.mockRejectedValue(new ApiRequestError('Failed to fetch', null, null))

    render(<App insideTelegram />)

    expect(await screen.findByText(strings.loadFailedFallback)).toBeInTheDocument()
  })

  it('speaks the language of the profile', async () => {
    fetchMeMock.mockResolvedValue(me({ language: 'en' }))

    render(<App insideTelegram />)

    expect(await screen.findByText(dictionaries.en.balanceTitle)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: dictionaries.en.tabMore })).toBeInTheDocument()
    applyLanguage('ru')
  })
})
