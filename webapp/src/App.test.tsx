import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from './api/client'
import type { Me } from './api/types'
import { App } from './App'
import { fetchMe } from './me/meApi'
import { strings } from './strings'

vi.mock('./me/meApi', () => ({ fetchMe: vi.fn() }))

const fetchMeMock = vi.mocked(fetchMe)

const me = (overrides: Partial<Me> = {}): Me => ({
  userId: 42,
  firstName: 'Амир',
  username: 'amir',
  language: 'ru',
  timezone: null,
  activeGroupId: '6650a1b2c3d4e5f6a7b8c9d0',
  subscription: {
    tier: 'FREE',
    expiresAt: null,
    limits: { aiRequestsPerDay: 10, categoriesPerType: 10, ownedSharedGroups: 3, activeNotifications: 3, historyDaysBack: 30 },
  },
  ...overrides,
})

describe('App', () => {
  beforeEach(() => {
    fetchMeMock.mockReset()
  })

  it('asks to open the app from Telegram and does not call the API outside it', () => {
    render(<App insideTelegram={false} />)

    expect(screen.getByText(strings.outsideTelegramTitle)).toBeInTheDocument()
    expect(fetchMeMock).not.toHaveBeenCalled()
  })

  it('shows loading, then greets the user by name with the tariff', async () => {
    fetchMeMock.mockResolvedValue(me())

    render(<App insideTelegram />)

    expect(screen.getByText(strings.loading)).toBeInTheDocument()
    expect(await screen.findByText(strings.greeting('Амир'))).toBeInTheDocument()
    expect(screen.getByText(strings.tier('FREE'))).toBeInTheDocument()
  })

  it('falls back to username when first name is missing', async () => {
    fetchMeMock.mockResolvedValue(me({ firstName: null }))

    render(<App insideTelegram />)

    expect(await screen.findByText(strings.greeting('amir'))).toBeInTheDocument()
  })

  it('sends the user to the bot when there is no group yet', async () => {
    fetchMeMock.mockResolvedValue(me({ activeGroupId: null }))

    render(<App insideTelegram />)

    expect(await screen.findByText(strings.noGroupHint)).toBeInTheDocument()
  })

  it('shows the server message and recovers on retry', async () => {
    const serverMessage = 'Не удалось подтвердить вход через Telegram.'
    fetchMeMock
      .mockRejectedValueOnce(new ApiRequestError(serverMessage, 401, { code: 'UNAUTHORIZED', message: serverMessage, details: {} }))
      .mockResolvedValueOnce(me())

    render(<App insideTelegram />)

    expect(await screen.findByText(strings.loadFailedTitle)).toBeInTheDocument()
    expect(screen.getByText(serverMessage)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: strings.retry }))

    expect(await screen.findByText(strings.greeting('Амир'))).toBeInTheDocument()
  })

  it('shows a generic hint when the network is down', async () => {
    fetchMeMock.mockRejectedValue(new ApiRequestError('Failed to fetch', null, null))

    render(<App insideTelegram />)

    expect(await screen.findByText(strings.loadFailedFallback)).toBeInTheDocument()
  })
})
