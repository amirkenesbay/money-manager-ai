import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { formatDayLabel, formatMoney, formatSignedMoney } from '../format'
import { strings } from '../strings'
import { asRendered } from '../test/text'
import { balance, category, FAMILY_GROUP_ID, group, me, operation, operationsPage, PERSONAL_GROUP_ID, reminder, summary } from '../test/fixtures'
import { MainScreen } from './MainScreen'
import { fetchCategories } from '../categories/categoriesApi'
import { fetchBalance, fetchMonthSummary, fetchRecentOperations } from '../dashboard/dashboardApi'
import { activateGroup, createGroup, fetchGroups, fetchInvite, fetchMembers } from '../groups/groupsApi'
import { fetchReminders } from '../reminders/remindersApi'
import { createOperation, deleteOperation, fetchOperations } from '../operations/operationsApi'
import { markHintSeen } from '../me/meApi'

vi.mock('../groups/groupsApi', () => ({ fetchGroups: vi.fn(), activateGroup: vi.fn(), createGroup: vi.fn(), fetchMembers: vi.fn(), fetchInvite: vi.fn() }))
vi.mock('../reminders/remindersApi', () => ({ fetchReminders: vi.fn() }))
vi.mock('../me/meApi', async (importOriginal) => ({ ...await importOriginal<object>(), markHintSeen: vi.fn() }))
vi.mock('../categories/categoriesApi', () => ({ fetchCategories: vi.fn() }))
vi.mock('../dashboard/dashboardApi', () => ({
  RECENT_OPERATIONS_LIMIT: 10,
  fetchBalance: vi.fn(),
  fetchMonthSummary: vi.fn(),
  fetchRecentOperations: vi.fn(),
}))

vi.mock('../operations/operationsApi', () => ({
  OPERATIONS_PAGE_SIZE: 20,
  NO_FILTERS: { from: null, to: null, type: null, categoryId: null },
  fetchOperations: vi.fn(),
  createOperation: vi.fn(),
  replaceOperation: vi.fn(),
  deleteOperation: vi.fn(),
  fetchRecentTags: () => Promise.resolve([]),
}))

const LANGUAGE = 'ru'
const familyGroup = group({ id: FAMILY_GROUP_ID, name: 'Семья', type: 'SHARED', isActive: false, memberCount: 3 })

const visible = (elements: HTMLElement[]) => elements.filter((element) => !element.closest('[hidden]'))
const renderDashboard = () => render(<MainScreen me={me()} activeGroupId={PERSONAL_GROUP_ID} onMeChanged={vi.fn()} />)

describe('MainScreen dashboard', () => {
  beforeAll(() => {
    Element.prototype.scrollTo = vi.fn()
  })

  afterEach(() => {
    localStorage.clear()
  })

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchGroups).mockResolvedValue([group()])
    vi.mocked(fetchBalance).mockResolvedValue(balance())
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary())
    vi.mocked(fetchRecentOperations).mockResolvedValue(operationsPage([operation()]))
    vi.mocked(activateGroup).mockResolvedValue(undefined)
    vi.mocked(fetchOperations).mockResolvedValue(operationsPage([operation()]))
    vi.mocked(fetchCategories).mockResolvedValue([])
    vi.mocked(fetchReminders).mockResolvedValue([])
    vi.mocked(fetchMembers).mockResolvedValue([])
  })

  it('shows the month spending, the income left and the balance', async () => {
    renderDashboard()

    const spent = within(await screen.findByRole('group', { name: strings.homeSlideNames[0] }))
    const balanceSlide = within(screen.getByRole('group', { name: strings.homeSlideNames[2] }))

    expect(spent.getByText(asRendered(formatMoney('15000', 'KZT', LANGUAGE)))).toBeInTheDocument()
    expect(spent.getByText(asRendered(strings.homeSpentMeta(formatMoney('300000', 'KZT', LANGUAGE), formatMoney('285000', 'KZT', LANGUAGE))))).toBeInTheDocument()
    expect(balanceSlide.getByText(asRendered(formatMoney('169999.5', 'KZT', LANGUAGE)))).toBeInTheDocument()
  })

  it('warns when the month expenses exceed the income', async () => {
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary({ income: '10000', expense: '22400', net: '-12400' }))

    renderDashboard()

    expect(await screen.findByText(asRendered(strings.monthOverspent(formatMoney('12400', 'KZT', LANGUAGE))))).toBeInTheDocument()
  })

  it('lists recent operations with sign and day', async () => {
    renderDashboard()

    expect(visible(await screen.findAllByText(asRendered(formatSignedMoney('EXPENSE', '3500', 'KZT', LANGUAGE))))).toHaveLength(1)
    expect(visible(screen.getAllByText(formatDayLabel('2026-10-12', new Date(), LANGUAGE)))).toHaveLength(1)
  })

  it('explains empty month and empty history', async () => {
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary({ income: '0', expense: '0', net: '0', operationCount: 0, expenseByCategory: [], dailyExpense: [] }))
    vi.mocked(fetchRecentOperations).mockResolvedValue(operationsPage([]))

    renderDashboard()

    expect(await screen.findByText(strings.homeNoIncome)).toBeInTheDocument()
    expect(screen.getByText(strings.homeSafeEmpty)).toBeInTheDocument()
    expect(screen.getByText(strings.recentOperationsEmpty)).toBeInTheDocument()
  })

  it('opens an income form from the daily allowance when there is no income yet', async () => {
    vi.mocked(fetchMonthSummary).mockResolvedValue(summary({ income: '0', net: '-15000' }))
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.homeAddIncome }))

    expect(await screen.findByRole('radio', { name: strings.operationType('INCOME') })).toHaveAttribute('aria-checked', 'true')
  })

  it('remembers the summary slide the user picked', async () => {
    const { unmount } = renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.homeSlideNames[2] }))
    unmount()
    renderDashboard()

    expect(await screen.findByRole('button', { name: strings.homeSlideNames[2] })).toHaveAttribute('aria-current', 'true')
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

  it('explains the microphone before the first voice recording', async () => {
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.recordVoice }))

    expect(screen.getByRole('dialog', { name: strings.voiceAskTitle })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.voiceAskLater }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
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

  it('opens the operation form when the bot reminder asks for it', async () => {
    window.history.replaceState(null, '', '/?screen=add')
    vi.mocked(fetchCategories).mockResolvedValue([category()])
    try {
      renderDashboard()

      expect(await screen.findByRole('textbox', { name: strings.amountLabel })).toBeInTheDocument()
    } finally {
      window.history.replaceState(null, '', '/')
    }
  })

  it('welcomes a newcomer once and starts voice from the last card', async () => {
    vi.mocked(markHintSeen).mockResolvedValue(me())
    render(<MainScreen me={me({ seenHints: [] })} activeGroupId={PERSONAL_GROUP_ID} onMeChanged={vi.fn()} />)

    expect(await screen.findByRole('heading', { name: strings.welcomeVoiceTitle })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeTryVoice }))

    expect(markHintSeen).toHaveBeenCalledWith('WELCOME')
    expect(screen.queryByRole('heading', { name: strings.welcomeVoiceTitle })).not.toBeInTheDocument()
  })

  it('keeps the welcome for later when the bot opens a form', async () => {
    window.history.replaceState(null, '', '/?screen=add')
    vi.mocked(fetchCategories).mockResolvedValue([category()])
    try {
      render(<MainScreen me={me({ seenHints: [] })} activeGroupId={PERSONAL_GROUP_ID} onMeChanged={vi.fn()} />)

      expect(await screen.findByRole('textbox', { name: strings.amountLabel })).toBeInTheDocument()
      expect(screen.queryByRole('heading', { name: strings.welcomeVoiceTitle })).not.toBeInTheDocument()
    } finally {
      window.history.replaceState(null, '', '/')
    }
  })

  it('shows the welcome again from More', async () => {
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.tabMore }))
    await userEvent.click(await screen.findByRole('button', { name: strings.howToUse }))

    expect(await screen.findByRole('heading', { name: strings.welcomeVoiceTitle })).toBeInTheDocument()
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

  it('opens settings from the More tab and the premium screen from there', async () => {
    vi.mocked(fetchReminders).mockResolvedValue([reminder()])
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.tabMore }))

    expect(await screen.findByRole('heading', { name: strings.moreTitle })).toBeInTheDocument()
    expect(await screen.findByText(strings.remindersOn(1))).toBeInTheDocument()
    expect(screen.getByText(strings.tierFree)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: strings.premiumButton }))

    expect(screen.getByRole('button', { name: strings.subscribeFor(formatMoney('990', 'KZT', LANGUAGE)) })).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: strings.tabBarLabel })).not.toBeInTheDocument()
  })

  it('switches the active group to a newly created one', async () => {
    const created = group({ id: 'g-new', name: 'Квартира', type: 'SHARED', isActive: true })
    vi.mocked(createGroup).mockResolvedValue(created)
    vi.mocked(fetchMembers).mockResolvedValue([])
    vi.mocked(fetchInvite).mockResolvedValue({ inviteToken: 'ABC', url: 'https://t.me/bot?start=join_ABC' })
    renderDashboard()

    await userEvent.click(await screen.findByRole('button', { name: strings.tabMore }))
    await userEvent.click(await screen.findByRole('button', { name: new RegExp(strings.groupsTitle) }))
    await userEvent.click(screen.getByRole('button', { name: strings.createGroup }))
    await userEvent.type(screen.getByRole('textbox', { name: strings.nameLabel }), 'Квартира')
    await userEvent.click(screen.getByRole('button', { name: strings.createGroup }))

    expect(await screen.findByText(strings.groupCreatedToast('Квартира'))).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.back }))
    await userEvent.click(screen.getByRole('button', { name: strings.back }))
    await userEvent.click(screen.getByRole('button', { name: strings.tabHome }))

    await vi.waitFor(() => expect(fetchBalance).toHaveBeenCalledWith('g-new', expect.anything()))
  })
})
