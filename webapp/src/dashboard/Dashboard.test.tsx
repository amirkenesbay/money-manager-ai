import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { formatMoney, formatSignedMoney } from '../format'
import { strings } from '../strings'
import { asRendered } from '../test/text'
import { balance, FAMILY_GROUP_ID, group, operation, operationsPage, PERSONAL_GROUP_ID, summary } from '../test/fixtures'
import { Dashboard } from './Dashboard'
import { activateGroup, fetchBalance, fetchGroups, fetchMonthSummary, fetchRecentOperations } from './dashboardApi'

vi.mock('./dashboardApi', () => ({
  RECENT_OPERATIONS_LIMIT: 10,
  fetchGroups: vi.fn(),
  activateGroup: vi.fn(),
  fetchBalance: vi.fn(),
  fetchMonthSummary: vi.fn(),
  fetchRecentOperations: vi.fn(),
}))

const LANGUAGE = 'ru'
const familyGroup = group({ id: FAMILY_GROUP_ID, name: 'Семья', type: 'SHARED', isActive: false, memberCount: 3 })

const renderDashboard = () => render(<Dashboard initialGroupId={PERSONAL_GROUP_ID} language={LANGUAGE} />)

describe('Dashboard', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchGroups).mockResolvedValue([group()])
    vi.mocked(fetchBalance).mockResolvedValue(balance())
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary())
    vi.mocked(fetchRecentOperations).mockResolvedValue(operationsPage([operation()]))
    vi.mocked(activateGroup).mockResolvedValue(undefined)
  })

  it('shows the balance with its breakdown', async () => {
    renderDashboard()

    expect(await screen.findByText(asRendered(formatMoney('169999.5', 'KZT', LANGUAGE)))).toBeInTheDocument()
    expect(screen.getByText(asRendered(formatMoney('50000', 'KZT', LANGUAGE)))).toBeInTheDocument()
    expect(screen.getByText(asRendered(formatSignedMoney('EXPENSE', '180000.5', 'KZT', LANGUAGE)))).toBeInTheDocument()
  })

  it('shows only the top three expense categories of the month', async () => {
    renderDashboard()

    const categories = await screen.findByRole('list', { name: strings.topExpensesTitle })
    expect(within(categories).getAllByRole('listitem')).toHaveLength(3)
    expect(within(categories).queryByText(/Кофе/)).not.toBeInTheDocument()
  })

  it('lists recent operations with sign and description', async () => {
    renderDashboard()

    expect(await screen.findByText(asRendered(formatSignedMoney('EXPENSE', '3500', 'KZT', LANGUAGE)))).toBeInTheDocument()
    expect(screen.getByText(/Магнум/)).toBeInTheDocument()
  })

  it('explains empty month and empty history', async () => {
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary({ operationCount: 0, expenseByCategory: [] }))
    vi.mocked(fetchRecentOperations).mockResolvedValue(operationsPage([]))

    renderDashboard()

    expect(await screen.findByText(strings.monthEmpty)).toBeInTheDocument()
    expect(screen.getByText(strings.recentOperationsEmpty)).toBeInTheDocument()
  })

  it('shows the group name as a title when there is only one group', async () => {
    renderDashboard()

    expect(await screen.findByRole('heading', { name: 'Личное' })).toBeInTheDocument()
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
  })

  it('switches the active group and reloads its data', async () => {
    vi.mocked(fetchGroups).mockResolvedValue([group(), familyGroup])
    renderDashboard()

    await userEvent.selectOptions(await screen.findByRole('combobox'), FAMILY_GROUP_ID)

    expect(activateGroup).toHaveBeenCalledWith(FAMILY_GROUP_ID)
    await vi.waitFor(() => expect(fetchBalance).toHaveBeenLastCalledWith(FAMILY_GROUP_ID, expect.anything()))
    expect(screen.getByRole('combobox')).toHaveValue(FAMILY_GROUP_ID)
  })

  it('keeps the current group and explains when switching fails', async () => {
    vi.mocked(fetchGroups).mockResolvedValue([group(), familyGroup])
    vi.mocked(activateGroup).mockRejectedValue(new ApiRequestError('Failed to fetch', null, null))
    renderDashboard()

    await userEvent.selectOptions(await screen.findByRole('combobox'), FAMILY_GROUP_ID)

    expect(await screen.findByText(strings.groupSwitchFailed)).toBeInTheDocument()
    expect(screen.getByRole('combobox')).toHaveValue(PERSONAL_GROUP_ID)
    expect(fetchBalance).not.toHaveBeenCalledWith(FAMILY_GROUP_ID, expect.anything())
  })

  it('recovers dashboard data on retry', async () => {
    vi.mocked(fetchBalance).mockRejectedValueOnce(new ApiRequestError('Failed to fetch', null, null))
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.retry }))

    expect(await screen.findByText(strings.balanceTitle)).toBeInTheDocument()
  })
})
