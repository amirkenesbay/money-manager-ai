import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import type { AnalyticsReport, CategoryReport, ComparisonReport, MembersReport } from '../api/types'
import { formatMoney, shiftMonth, toIsoMonth } from '../format'
import { fetchCategories } from '../categories/categoriesApi'
import { strings } from '../strings'
import { category, FAMILY_GROUP_ID, group, me, PERSONAL_GROUP_ID } from '../test/fixtures'
import { asRendered } from '../test/text'
import { fetchAnalytics, fetchCategoryReport, fetchComparison, fetchMembers } from './reportsApi'
import { ReportsScreen } from './ReportsScreen'

vi.mock('./reportsApi', () => ({
  fetchAnalytics: vi.fn(),
  fetchComparison: vi.fn(),
  fetchMembers: vi.fn(),
  fetchCategoryReport: vi.fn(),
}))
vi.mock('../categories/categoriesApi', () => ({ fetchCategories: vi.fn() }))
vi.mock('../telegram/telegramEnv', () => ({ openExternalLink: vi.fn(), rawInitData: () => undefined }))

const LANGUAGE = 'ru'
const CURRENT_MONTH = toIsoMonth(new Date())
const premiumRequired = new ApiRequestError('limit', 422, { code: 'PREMIUM_REQUIRED', message: 'limit', details: {} })

const analytics = (overrides: Partial<AnalyticsReport> = {}): AnalyticsReport => ({
  month: CURRENT_MONTH,
  currency: 'KZT',
  income: '450000',
  expense: '48200',
  operationCount: 12,
  daysInMonth: 31,
  topExpenses: [
    { icon: '🛒', name: 'Продукты', total: '21000' },
    { icon: '🚕', name: 'Транспорт', total: '9000' },
    { icon: '🎬', name: 'Кино', total: '7600' },
  ],
  maxExpense: null,
  mostExpensiveDay: null,
  ...overrides,
})

const comparison: ComparisonReport = {
  month: CURRENT_MONTH,
  previousMonth: shiftMonth(CURRENT_MONTH, -1),
  currency: 'KZT',
  income: { previous: '450000', current: '450000' },
  expense: { previous: '50000', current: '44000' },
  categories: [],
}

const members: MembersReport = {
  month: CURRENT_MONTH,
  currency: 'KZT',
  income: '0',
  expense: '48200',
  incomeByMember: [],
  expenseByMember: [{ name: 'Амир', total: '32000' }, { name: 'Айгерим', total: '16200' }],
}

const categoryReport: CategoryReport = {
  categoryId: 'cat-1',
  icon: '🛒',
  name: 'Продукты',
  currency: 'KZT',
  months: [
    { month: shiftMonth(CURRENT_MONTH, -1), total: '20000', count: 4 },
    { month: CURRENT_MONTH, total: '30000', count: 6 },
  ],
}

const onAddOperation = vi.fn()
const renderReports = (reportGroup = group()) => render(<ReportsScreen me={me()} group={reportGroup} onAddOperation={onAddOperation} />)
const openTab = (tab: Parameters<typeof strings.reportTab>[0]) => userEvent.click(screen.getByRole('radio', { name: strings.reportTab(tab) }))
const money = (amount: string) => asRendered(formatMoney(amount, 'KZT', LANGUAGE))

describe('ReportsScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchAnalytics).mockResolvedValue(analytics())
    vi.mocked(fetchComparison).mockResolvedValue(comparison)
    vi.mocked(fetchMembers).mockResolvedValue(members)
    vi.mocked(fetchCategoryReport).mockResolvedValue(categoryReport)
    vi.mocked(fetchCategories).mockResolvedValue([category(), category({ id: 'cat-9', name: 'Зарплата', type: 'INCOME' })])
  })

  it('splits the month expenses by category and groups the rest', async () => {
    renderReports()

    expect(await screen.findByRole('img', { name: /по категориям/ })).toBeInTheDocument()
    expect(screen.getByText(money('48200'))).toBeInTheDocument()
    expect(screen.getByText('🛒 Продукты')).toBeInTheDocument()
    expect(screen.getByText(strings.otherCategories)).toBeInTheDocument()
    expect(screen.getByText(money('10600'))).toBeInTheDocument()
    expect(fetchAnalytics).toHaveBeenCalledWith(PERSONAL_GROUP_ID, CURRENT_MONTH, expect.anything())
  })

  it('moves between months but not into the future', async () => {
    renderReports()
    await screen.findByText(strings.otherCategories)

    expect(screen.getByRole('button', { name: strings.nextMonth })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: strings.previousMonth }))

    expect(fetchAnalytics).toHaveBeenLastCalledWith(PERSONAL_GROUP_ID, shiftMonth(CURRENT_MONTH, -1), expect.anything())
    expect(screen.getByRole('button', { name: strings.nextMonth })).toBeEnabled()
  })

  it('offers Premium for months beyond the free history and returns to the current one', async () => {
    renderReports()
    await screen.findByText(strings.otherCategories)
    vi.mocked(fetchAnalytics).mockRejectedValueOnce(premiumRequired)

    await userEvent.click(screen.getByRole('button', { name: strings.previousMonth }))
    await userEvent.click(await screen.findByRole('button', { name: strings.backToCurrentMonth }))

    expect(fetchAnalytics).toHaveBeenLastCalledWith(PERSONAL_GROUP_ID, CURRENT_MONTH, expect.anything())
    expect(await screen.findByText(strings.otherCategories)).toBeInTheDocument()
  })

  it('invites to record an operation when the month is empty', async () => {
    vi.mocked(fetchAnalytics).mockResolvedValue(analytics({ operationCount: 0, expense: '0', topExpenses: [] }))
    renderReports()

    await userEvent.click(await screen.findByRole('button', { name: strings.addFirstOperation }))

    expect(onAddOperation).toHaveBeenCalled()
  })

  it('compares the month with the previous one', async () => {
    renderReports()
    await openTab('comparison')

    expect(await screen.findByText('−12%')).toBeInTheDocument()
    expect(screen.getByText('0%')).toBeInTheDocument()
    expect(fetchComparison).toHaveBeenCalledWith(PERSONAL_GROUP_ID, CURRENT_MONTH, expect.anything())
  })

  it('shows member spending and explains the personal group', async () => {
    renderReports()
    await openTab('members')

    expect(await screen.findByText('Айгерим')).toBeInTheDocument()
    expect(screen.getByText(strings.membersSolo)).toBeInTheDocument()
  })

  it('does not explain the personal group in a shared one', async () => {
    renderReports(group({ id: FAMILY_GROUP_ID, name: 'Семья', type: 'SHARED', memberCount: 2 }))
    await openTab('members')

    expect(await screen.findByText('Айгерим')).toBeInTheDocument()
    expect(screen.queryByText(strings.membersSolo)).not.toBeInTheDocument()
  })

  it('charts the first expense category by month with the average', async () => {
    renderReports()
    await openTab('category')

    expect(await screen.findByRole('img', { name: strings.categoryChart('🛒 Продукты') })).toBeInTheDocument()
    expect(screen.getByText(money('25000'))).toBeInTheDocument()
    expect(fetchCategoryReport).toHaveBeenCalledWith(PERSONAL_GROUP_ID, 'cat-1', expect.anything())
    expect(screen.queryByRole('button', { name: strings.previousMonth })).not.toBeInTheDocument()
  })
})
