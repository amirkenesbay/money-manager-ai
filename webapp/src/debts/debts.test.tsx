import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Debts, Member } from '../api/types'
import { formatMoney } from '../format'
import { fetchMembers } from '../groups/groupsApi'
import { strings } from '../strings'
import { group, me } from '../test/fixtures'
import { asRendered } from '../test/text'
import { fetchDebts, recordSettlement, remindDebtor, undoSettlement } from './debtsApi'
import { DebtsScreen } from './DebtsScreen'
import { DebtsSummary } from './DebtsSummary'
import { impactOf, partsOf } from './share'
import { SplitField } from './SplitField'
import { createLoan, fetchLoanPeople, repayLoan } from './loansApi'
import type { Loan } from '../api/types'

vi.mock('./debtsApi', () => ({ fetchDebts: vi.fn(), recordSettlement: vi.fn(), remindDebtor: vi.fn(), undoSettlement: vi.fn() }))
vi.mock('../groups/groupsApi', () => ({ fetchMembers: vi.fn() }))
vi.mock('./loansApi', () => ({ fetchLoanPeople: vi.fn(), createLoan: vi.fn(), repayLoan: vi.fn(), undoRepayment: vi.fn(), deleteLoan: vi.fn() }))

const ME = me().userId
const ANYA = ME + 1
const DANYA = ME + 2
const MEMBERS: Member[] = [
  { userId: ME, name: 'Амир', isOwner: true },
  { userId: ANYA, name: 'Аня', isOwner: false },
  { userId: DANYA, name: 'Даня', isOwner: false },
]
const shared = group({ id: 'family', type: 'SHARED', splitExpenses: true })
const money = (amount: string) => formatMoney(amount, shared.currency, me().language)
const debts = (overrides: Partial<Debts> = {}): Debts => ({
  balance: '8500',
  owedToMe: '12000',
  iOwe: '3500',
  loans: [],
  mine: [
    { userId: ANYA, name: 'Аня', amount: '12000', items: [{ kind: 'EXPENSE', amount: '12000', date: '2026-11-02', operationId: 'op-1', icon: '🍔', title: 'Кафе' }] },
    { userId: DANYA, name: 'Даня', amount: '-3500', items: [{ kind: 'OFFSET', amount: '-3500', date: null, operationId: null, icon: null, title: null }] },
  ],
  others: [],
  history: [],
  ...overrides,
})

describe('splitting', () => {
  it('gives the odd tenge to the payer', () => {
    const parts = partsOf(100, { payerId: ANYA, participantIds: [ME, ANYA, DANYA] })

    expect(parts.get(ME)).toBe(33.33)
    expect(parts.get(ANYA)).toBe(33.34)
  })

  it('tells what an expense means for me', () => {
    expect(impactOf(3000, { payerId: ME, participantIds: [ME, ANYA, DANYA] }, ME)).toEqual({ kind: 'owed', amount: 2000 })
    expect(impactOf(3000, { payerId: ANYA, participantIds: [ME, ANYA] }, ME)).toEqual({ kind: 'mine', amount: 1500 })
    expect(impactOf(3000, { payerId: ANYA, participantIds: [ANYA, DANYA] }, ME)).toEqual({ kind: 'none' })
  })

  it('picks who paid and for whom and shows my part', async () => {
    const onChange = vi.fn()
    const view = render(<SplitField members={MEMBERS} userId={ME} amount={3000} currency="KZT" language="ru" share={{ payerId: ME, participantIds: [ME, ANYA, DANYA] }} onChange={onChange} />)

    expect(screen.getByRole('status')).toHaveTextContent(asRendered(strings.shareOwed(formatMoney('2000', 'KZT', 'ru'))))
    await userEvent.click(screen.getByRole('button', { name: new RegExp(strings.paidByLabel) }))
    await userEvent.click(screen.getByRole('radio', { name: 'Даня' }))
    expect(onChange).toHaveBeenLastCalledWith({ payerId: DANYA, participantIds: [ME, ANYA, DANYA] })

    view.rerender(<SplitField members={MEMBERS} userId={ME} amount={3000} currency="KZT" language="ru" share={{ payerId: DANYA, participantIds: [ME, ANYA, DANYA] }} onChange={onChange} />)
    await userEvent.click(screen.getByRole('button', { name: new RegExp(strings.paidForLabel) }))
    await userEvent.click(screen.getByRole('checkbox', { name: /Аня/ }))
    await userEvent.click(screen.getByRole('button', { name: strings.done }))
    expect(onChange).toHaveBeenLastCalledWith({ payerId: DANYA, participantIds: [ME, DANYA] })
  })
})

describe('debts', () => {
  const notify = vi.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchMembers).mockResolvedValue(MEMBERS)
    vi.mocked(fetchDebts).mockResolvedValue(debts())
  })

  it('shows what I am owed on the home screen', async () => {
    render(<DebtsSummary groupId={shared.id} currency={shared.currency} language="ru" showSettled onOpen={vi.fn()} />)

    expect(await screen.findByText(strings.debtsOwedToMe)).toBeInTheDocument()
    expect(screen.getByText(asRendered(money('8500')))).toBeInTheDocument()
  })

  it('says there are no debts when everyone is even', async () => {
    vi.mocked(fetchDebts).mockResolvedValue(debts({ balance: '0', mine: [] }))
    render(<DebtsSummary groupId={shared.id} currency={shared.currency} language="ru" showSettled onOpen={vi.fn()} />)

    expect(await screen.findByText(strings.debtsNone)).toBeInTheDocument()
  })

  it('reminds a debtor and explains where the debt came from', async () => {
    vi.mocked(remindDebtor).mockResolvedValue(undefined)
    render(<DebtsScreen me={me()} group={shared} notify={notify} onBack={vi.fn()} />)

    await userEvent.click(await screen.findByRole('button', { name: new RegExp('Аня') }))
    expect(screen.getByText(/🍔 Кафе/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.debtRemind }))

    expect(remindDebtor).toHaveBeenCalledWith(shared.id, ANYA)
    expect(notify).toHaveBeenCalledWith({ title: strings.debtReminded })
  })

  it('records a partial repayment and offers to undo it', async () => {
    vi.mocked(recordSettlement).mockResolvedValue({ id: 's-1', fromId: ME, fromName: 'Амир', toId: DANYA, toName: 'Даня', amount: '2000', date: '2026-11-05', canUndo: true })
    vi.mocked(undoSettlement).mockResolvedValue(undefined)
    render(<DebtsScreen me={me()} group={shared} notify={notify} onBack={vi.fn()} />)

    await userEvent.click(await screen.findByRole('tab', { name: new RegExp(strings.debtsTabMine) }))
    await userEvent.click(screen.getByRole('button', { name: strings.debtPaid }))
    const amount = screen.getByRole('textbox', { name: strings.debtAmountTitle })
    await userEvent.clear(amount)
    await userEvent.type(amount, '2000')
    await userEvent.click(screen.getByRole('button', { name: strings.save }))

    expect(recordSettlement).toHaveBeenCalledWith(shared.id, ME, DANYA, '2000')
    const toast = notify.mock.calls.at(-1)?.[0]
    expect(toast.title).toBe(strings.debtSaved)
    toast.action.onClick()
    expect(undoSettlement).toHaveBeenCalledWith(shared.id, 's-1')
  })

  it('celebrates when there are no debts left', async () => {
    vi.mocked(fetchDebts).mockResolvedValue(debts({ balance: '0', mine: [] }))
    render(<DebtsScreen me={me()} group={shared} notify={notify} onBack={vi.fn()} />)

    expect(await screen.findByRole('heading', { name: strings.debtsNone })).toBeInTheDocument()
  })
})

describe('personal loans', () => {
  const notify = vi.fn()
  const loan = (overrides: Partial<Loan> = {}): Loan => ({
    id: 'loan-1', direction: 'LENT', person: 'Ерлан', amount: '20000', repaid: '5000', remaining: '15000', lentOn: '2026-11-01',
    dueDate: '2020-01-01', note: 'на ремонт', repayments: [{ id: 'rep-1', amount: '5000', date: '2026-11-03' }], open: true, ...overrides,
  })

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchMembers).mockResolvedValue(MEMBERS)
    vi.mocked(fetchLoanPeople).mockResolvedValue(['Ерлан', 'Мама'])
    vi.mocked(fetchDebts).mockResolvedValue(debts({
      mine: [],
      owedToMe: '15000',
      iOwe: '50000',
      loans: [loan(), loan({ id: 'loan-2', direction: 'BORROWED', person: 'Мама', amount: '50000', repaid: '0', remaining: '50000', dueDate: null, repayments: [] })],
    }))
  })

  it('splits loans into what I am owed and what I owe, overdue ones marked', async () => {
    render(<DebtsScreen me={me()} group={shared} notify={notify} onBack={vi.fn()} />)

    expect(await screen.findByRole('button', { name: /Ерлан/ })).toHaveTextContent(/просрочено/)
    expect(screen.queryByRole('button', { name: /Мама/ })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: new RegExp(strings.debtsTabMine) }))

    expect(screen.getByRole('button', { name: /Мама/ })).toBeInTheDocument()
  })

  it('takes everything back from a loan and says it is closed', async () => {
    vi.mocked(repayLoan).mockResolvedValue(loan({ repaid: '20000', remaining: '0', open: false, repayments: [{ id: 'rep-2', amount: '15000', date: '2026-11-05' }] }))
    render(<DebtsScreen me={me()} group={shared} notify={notify} onBack={vi.fn()} />)

    await userEvent.click(await screen.findByRole('button', { name: /Ерлан/ }))
    await userEvent.click(screen.getByRole('button', { name: strings.loanRepay }))
    await userEvent.click(screen.getByRole('button', { name: new RegExp(strings.loanAll('').trim()) }))
    await userEvent.click(screen.getByRole('button', { name: strings.save }))

    expect(repayLoan).toHaveBeenCalledWith(shared.id, 'loan-1', '15000')
    expect(notify).toHaveBeenCalledWith(expect.objectContaining({ title: strings.loanClosed }))
  })

  it('records a new loan to a person picked from earlier ones', async () => {
    vi.mocked(createLoan).mockResolvedValue(loan({ id: 'loan-3', person: 'Мама', amount: '7000' }))
    render(<DebtsScreen me={me()} group={shared} notify={notify} onBack={vi.fn()} />)

    await userEvent.click(await screen.findByRole('button', { name: strings.loanAdd }))
    await userEvent.type(screen.getByRole('textbox', { name: strings.amountLabel }), '7000')
    await userEvent.click(await screen.findByRole('button', { name: 'Мама' }))
    await userEvent.click(screen.getByRole('button', { name: strings.loanWeek }))
    await userEvent.click(screen.getByRole('button', { name: strings.save }))

    expect(createLoan).toHaveBeenCalledWith(shared.id, expect.objectContaining({ direction: 'LENT', person: 'Мама', amount: '7000', dueDate: expect.any(String) }))
    expect(notify).toHaveBeenCalledWith(expect.objectContaining({ title: strings.loanSaved }))
  })
})
