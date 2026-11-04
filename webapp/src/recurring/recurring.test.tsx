import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { RecurringPayment } from '../api/types'
import { fetchCategories } from '../categories/categoriesApi'
import { formatSignedMoney } from '../format'
import { strings } from '../strings'
import { category, group, me } from '../test/fixtures'
import { asRendered } from '../test/text'
import { describeRepeat } from './recurrence'
import { createRecurring, fetchRecurring, setRecurringActive } from './recurringApi'
import { RecurringScreen } from './RecurringScreen'

vi.mock('./recurringApi', () => ({ fetchRecurring: vi.fn(), createRecurring: vi.fn(), updateRecurring: vi.fn(), setRecurringActive: vi.fn(), deleteRecurring: vi.fn() }))
vi.mock('../categories/categoriesApi', () => ({ fetchCategories: vi.fn() }))
vi.mock('../me/meApi', () => ({ updateMe: vi.fn() }))

const notify = vi.fn()
const netflix: RecurringPayment = {
  id: 'rec-1', type: 'EXPENSE', amount: '3500', categoryId: 'cat-1', categoryName: 'Продукты', categoryIcon: '📺', description: 'Netflix',
  unit: 'MONTH', interval: 1, startDate: '2026-11-15', endDate: null, nextDate: '2026-12-15', mode: 'AUTO', active: true, monthlyAmount: '3500.00',
}
const renderScreen = () => render(<RecurringScreen me={me()} group={group()} onMeChanged={vi.fn()} notify={notify} onBack={vi.fn()} />)

describe('describeRepeat', () => {
  it('says how often in plain words', () => {
    expect(describeRepeat({ unit: 'MONTH', interval: 1 })).toBe(strings.scheduleEveryMonth)
    expect(describeRepeat({ unit: 'WEEK', interval: 2 })).toBe(strings.scheduleEvery(2, 'weeks'))
  })
})

describe('RecurringScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchRecurring).mockResolvedValue({ items: [netflix], monthlyExpense: '3500.00', monthlyIncome: '0' })
    vi.mocked(fetchCategories).mockResolvedValue([category()])
  })

  it('lists payments with what they cost a month', async () => {
    renderScreen()

    expect(await screen.findByText('Netflix')).toBeInTheDocument()
    expect(screen.getByText(asRendered(strings.recurringPerMonth(formatSignedMoney('EXPENSE', '3500.00', 'KZT', 'ru'))))).toBeInTheDocument()
  })

  it('pauses a payment', async () => {
    vi.mocked(setRecurringActive).mockResolvedValue({ ...netflix, active: false })
    renderScreen()

    await userEvent.click(await screen.findByRole('switch', { name: strings.recurringToggleLabel('Netflix') }))

    expect(setRecurringActive).toHaveBeenCalledWith(group().id, 'rec-1', false)
  })

  it('creates a monthly payment from the form', async () => {
    vi.mocked(createRecurring).mockResolvedValue(netflix)
    renderScreen()

    await userEvent.click(await screen.findByRole('button', { name: strings.addRecurring }))
    await userEvent.type(screen.getByRole('textbox', { name: strings.amountLabel }), '3500')
    await userEvent.type(screen.getByRole('textbox', { name: strings.commentLabel }), 'Netflix')
    await userEvent.click(await screen.findByRole('button', { name: /Продукты/ }))
    await userEvent.click(screen.getByRole('button', { name: strings.save }))

    expect(createRecurring).toHaveBeenCalledWith(group().id, expect.objectContaining({ amount: '3500', description: 'Netflix', unit: 'MONTH', interval: 1, mode: 'AUTO' }))
    expect(notify).toHaveBeenCalledWith({ title: strings.recurringCreatedToast, detail: 'Netflix' })
  })

  it('sends a free user at the limit to Premium instead of the form', async () => {
    vi.mocked(fetchRecurring).mockResolvedValue({ items: [netflix, { ...netflix, id: 'rec-2' }, { ...netflix, id: 'rec-3' }], monthlyExpense: '10500', monthlyIncome: '0' })
    renderScreen()

    await userEvent.click(await screen.findByRole('button', { name: strings.addRecurring }))

    expect(screen.getByText(strings.recurringLimitTitle)).toBeInTheDocument()
  })
})
