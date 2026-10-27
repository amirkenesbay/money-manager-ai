import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { formatDayLabel, formatMoney, formatSignedMoney } from '../format'
import { strings } from '../strings'
import { asRendered } from '../test/text'
import { balance, category, FAMILY_GROUP_ID, group, me, operation, operationsPage, PERSONAL_GROUP_ID, summary } from '../test/fixtures'
import { MainScreen } from './MainScreen'
import { activateGroup, fetchBalance, fetchGroups, fetchMonthSummary, fetchRecentOperations } from '../dashboard/dashboardApi'
import { createOperation, deleteOperation, fetchCategories, fetchOperations } from '../operations/operationsApi'

vi.mock('../dashboard/dashboardApi', () => ({
  RECENT_OPERATIONS_LIMIT: 10,
  fetchGroups: vi.fn(),
  activateGroup: vi.fn(),
  fetchBalance: vi.fn(),
  fetchMonthSummary: vi.fn(),
  fetchRecentOperations: vi.fn(),
}))

vi.mock('../operations/operationsApi', () => ({
  OPERATIONS_PAGE_SIZE: 20,
  NO_FILTERS: { from: null, to: null, type: null, categoryId: null },
  fetchOperations: vi.fn(),
  fetchCategories: vi.fn(),
  createOperation: vi.fn(),
  replaceOperation: vi.fn(),
  deleteOperation: vi.fn(),
}))

const LANGUAGE = 'ru'
const familyGroup = group({ id: FAMILY_GROUP_ID, name: 'Семья', type: 'SHARED', isActive: false, memberCount: 3 })

const renderDashboard = () => render(<MainScreen me={me()} activeGroupId={PERSONAL_GROUP_ID} />)

describe('MainScreen dashboard', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchGroups).mockResolvedValue([group()])
    vi.mocked(fetchBalance).mockResolvedValue(balance())
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary())
    vi.mocked(fetchRecentOperations).mockResolvedValue(operationsPage([operation()]))
    vi.mocked(activateGroup).mockResolvedValue(undefined)
    vi.mocked(fetchOperations).mockResolvedValue(operationsPage([operation()]))
    vi.mocked(fetchCategories).mockResolvedValue([])
  })

  it('shows the balance with the month expense and income', async () => {
    renderDashboard()

    expect(await screen.findByText(asRendered(formatMoney('169999.5', 'KZT', LANGUAGE)))).toBeInTheDocument()
    expect(screen.getByText(asRendered(strings.monthExpense(formatSignedMoney('EXPENSE', '15000', 'KZT', LANGUAGE))))).toBeInTheDocument()
    expect(screen.getByText(asRendered(strings.monthIncome(formatSignedMoney('INCOME', '300000', 'KZT', LANGUAGE))))).toBeInTheDocument()
  })

  it('warns when the month expenses exceed the income', async () => {
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary({ income: '10000', expense: '22400', net: '-12400' }))

    renderDashboard()

    expect(await screen.findByText(asRendered(strings.monthOverspent(formatMoney('12400', 'KZT', LANGUAGE))))).toBeInTheDocument()
  })

  it('lists recent operations with sign and day', async () => {
    renderDashboard()

    expect(await screen.findByText(asRendered(formatSignedMoney('EXPENSE', '3500', 'KZT', LANGUAGE)))).toBeInTheDocument()
    expect(screen.getByText(formatDayLabel('2026-10-12', new Date(), LANGUAGE))).toBeInTheDocument()
  })

  it('explains empty month and empty history', async () => {
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary({ operationCount: 0, expenseByCategory: [] }))
    vi.mocked(fetchRecentOperations).mockResolvedValue(operationsPage([]))

    renderDashboard()

    expect(await screen.findByText(strings.monthNoExpense)).toBeInTheDocument()
    expect(screen.getByText(strings.recentOperationsEmpty)).toBeInTheDocument()
  })

  it('shows the group name without a picker when there is only one group', async () => {
    renderDashboard()

    expect(await screen.findByText('Личное')).toBeInTheDocument()
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

  it('tells that voice entry is coming soon', async () => {
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.recordVoice }))

    expect(screen.getByRole('status')).toHaveTextContent(strings.comingSoon)
  })

  it('records an operation manually, refreshes the data and lets the user undo it', async () => {
    vi.mocked(fetchCategories).mockResolvedValue([category()])
    vi.mocked(createOperation).mockResolvedValue(operation({ id: 'new-op', amount: '700' }))
    vi.mocked(deleteOperation).mockResolvedValue(undefined)
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.manualEntryLabel }))
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '700')
    await userEvent.click(screen.getByRole('button', { name: strings.save }))

    expect(await screen.findByText(strings.savedToast)).toBeInTheDocument()
    await vi.waitFor(() => expect(fetchBalance).toHaveBeenCalledTimes(2))

    await userEvent.click(screen.getByRole('button', { name: strings.undo }))

    expect(deleteOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, 'new-op')
    expect(await screen.findByText(strings.undoneToast)).toBeInTheDocument()
  })

  it('opens a recent operation and returns home with the back control', async () => {
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: /Продукты/ }))
    expect(screen.getByRole('heading', { name: strings.operationTitle })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: strings.back }))

    expect(screen.queryByRole('heading', { name: strings.operationTitle })).not.toBeInTheDocument()
    expect(screen.getByText(strings.balanceTitle)).toBeVisible()
  })

  it('recovers dashboard data on retry', async () => {
    vi.mocked(fetchBalance).mockRejectedValueOnce(new ApiRequestError('Failed to fetch', null, null))
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.retry }))

    expect(await screen.findByText(strings.balanceTitle)).toBeInTheDocument()
  })

  it('opens all operations of the active group and returns home from the tab bar', async () => {
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.allOperations }))

    expect(await screen.findByRole('heading', { name: strings.operationsTitle })).toBeInTheDocument()
    expect(fetchOperations).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.anything(), 0, expect.anything())
    expect(screen.getByRole('button', { name: strings.operationsTitle })).toHaveAttribute('aria-current', 'page')

    await userEvent.click(screen.getByRole('button', { name: strings.tabHome }))

    expect(await screen.findByText(strings.balanceTitle)).toBeInTheDocument()
  })
})
