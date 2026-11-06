import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { formatMoney, toIsoDate } from '../format'
import { createCategory, fetchCategories } from '../categories/categoriesApi'
import { createOperation, fetchRecentTags, replaceOperation } from '../operations/operationsApi'
import { createRecurring } from '../recurring/recurringApi'
import { fetchMembers } from '../groups/groupsApi'
import { strings } from '../strings'
import { category, group, me, operation, PERSONAL_GROUP_ID } from '../test/fixtures'
import { asRendered } from '../test/text'
import { OperationForm } from './OperationForm'

vi.mock('../operations/operationsApi', () => ({
  createOperation: vi.fn(),
  replaceOperation: vi.fn(),
  deleteOperation: vi.fn(),
  fetchRecentTags: vi.fn(),
}))

vi.mock('../categories/categoriesApi', () => ({ fetchCategories: vi.fn(), createCategory: vi.fn() }))
vi.mock('../recurring/recurringApi', () => ({ createRecurring: vi.fn() }))
vi.mock('../groups/groupsApi', () => ({ fetchMembers: vi.fn() }))

const onSaved = vi.fn()
const today = () => toIsoDate(new Date())
const daysAgo = (days: number) => {
  const now = new Date()
  return toIsoDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() - days))
}
const renderForm = (editing: ReturnType<typeof operation> | null = null) =>
  render(<OperationForm group={group()} userId={me().userId} language="ru" operation={editing} onBack={vi.fn()} onSaved={onSaved} onDeleted={vi.fn()} />)
const amountInput = () => screen.getByRole('textbox', { name: strings.amountLabel })
const saveButton = (name = strings.save) => screen.getByRole('button', { name })

describe('OperationForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchRecentTags).mockResolvedValue(['ремонт', 'отпуск'])
    vi.mocked(fetchCategories).mockResolvedValue([
      category(),
      category({ id: 'cat-2', name: 'Такси', icon: '🚕' }),
      category({ id: 'cat-3', name: 'Зарплата', icon: '💰', type: 'INCOME' }),
    ])
    vi.mocked(createOperation).mockImplementation(async (_, input) => operation({ id: 'new', ...input, description: input.description }))
  })

  it('records an expense with the chosen category, day and comment', async () => {
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '5 000')
    await userEvent.click(screen.getByRole('button', { name: /Такси/ }))
    await userEvent.click(screen.getByRole('button', { name: strings.quickDate(1) }))
    await userEvent.type(screen.getByRole('textbox', { name: strings.commentLabel }), '  до работы ')

    await userEvent.click(saveButton())

    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, {
      type: 'EXPENSE', amount: '5000', categoryId: 'cat-2', operationDate: daysAgo(1), description: 'до работы',
    })
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ created: true }))
  })

  it('creates a missing category right from the picker and records into it', async () => {
    vi.mocked(createCategory).mockImplementation(async (_, { name, type }) => category({ id: 'cat-new', name, type }))
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '1200')
    await userEvent.click(screen.getByRole('button', { name: strings.addCategory }))
    const sheet = screen.getByRole('dialog', { name: strings.newCategoryTitle })
    await userEvent.type(within(sheet).getByRole('textbox', { name: strings.nameLabel }), 'Кафе')
    await userEvent.click(within(sheet).getByRole('button', { name: strings.save }))

    expect(createCategory).toHaveBeenCalledWith(PERSONAL_GROUP_ID, { name: 'Кафе', icon: '🛒', type: 'EXPENSE' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Кафе/ })).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(saveButton())
    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ categoryId: 'cat-new' }))
  })

  it('records who paid and for whom in a group that splits expenses', async () => {
    const me = 42
    vi.mocked(fetchMembers).mockResolvedValue([{ userId: me, name: 'Dev', isOwner: true }, { userId: 43, name: 'Аня', isOwner: false }])
    render(<OperationForm group={group({ id: 'family', type: 'SHARED', splitExpenses: true })} userId={me} language="ru" operation={null} onBack={vi.fn()} onSaved={onSaved} onDeleted={vi.fn()} />)
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '3000')
    await userEvent.click(await screen.findByRole('button', { name: new RegExp(strings.paidByLabel) }))
    await userEvent.click(screen.getByRole('radio', { name: 'Аня' }))

    expect(screen.getByRole('status')).toHaveTextContent(asRendered(strings.shareMine(formatMoney('1500', 'KZT', 'ru'))))
    await userEvent.click(saveButton())

    expect(createOperation).toHaveBeenCalledWith('family', expect.objectContaining({ payerId: 43, participantIds: [me, 43] }))
  })

  it('adds a typed tag and a recent one to the description', async () => {
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '1500')
    await userEvent.click(screen.getByRole('button', { name: /Такси/ }))
    await userEvent.type(screen.getByRole('textbox', { name: strings.commentLabel }), 'кофе')
    await userEvent.click(screen.getByRole('button', { name: strings.addTag }))
    await userEvent.type(screen.getByRole('textbox', { name: strings.newTagLabel }), 'Новый год{Enter}')
    await userEvent.click(await screen.findByRole('button', { name: strings.suggestTag('отпуск') }))

    await userEvent.click(saveButton())

    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ description: 'кофе #новый_год #отпуск' }))
  })

  it('shows the tags of an operation apart from its comment and lets the user remove one', async () => {
    vi.mocked(replaceOperation).mockResolvedValue(operation())
    renderForm(operation({ description: 'Магнум #отпуск #море' }))

    expect(await screen.findByRole('textbox', { name: strings.commentLabel })).toHaveValue('Магнум')
    await userEvent.click(screen.getByRole('button', { name: strings.removeTag('море') }))
    await userEvent.click(saveButton(strings.saveChanges))

    expect(replaceOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.any(String), expect.objectContaining({ description: 'Магнум #отпуск' }))
  })

  it('makes a new operation repeat every month', async () => {
    const saved = operation({ id: 'op-new', amount: '3500', categoryId: 'cat-2', operationDate: today(), description: 'Netflix' })
    vi.mocked(createOperation).mockResolvedValue(saved)
    vi.mocked(createRecurring).mockResolvedValue({} as Awaited<ReturnType<typeof createRecurring>>)
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '3500')
    await userEvent.click(screen.getByRole('button', { name: /Такси/ }))
    await userEvent.click(screen.getByRole('radio', { name: strings.scheduleEveryMonth }))

    await userEvent.click(saveButton())

    expect(createRecurring).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ unit: 'MONTH', interval: 1, startDate: today(), firstRecorded: true, mode: 'AUTO' }))
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ note: strings.repeatSavedToast(strings.scheduleEveryMonth) }))
  })

  it('offers only categories of the chosen type', async () => {
    renderForm()
    await screen.findByRole('button', { name: /Такси/ })

    await userEvent.click(screen.getByRole('radio', { name: strings.operationType('INCOME') }))

    expect(screen.getByRole('button', { name: /Зарплата/ })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByRole('button', { name: /Такси/ })).not.toBeInTheDocument()
  })

  it('blocks saving a zero amount and explains why', async () => {
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '0')

    expect(screen.getByRole('alert')).toHaveTextContent(strings.amountInvalid)
    await userEvent.click(saveButton())

    expect(createOperation).not.toHaveBeenCalled()
  })

  it('shakes the amount when saving without one', async () => {
    renderForm()
    await userEvent.click(await screen.findByRole('button', { name: strings.save }))

    expect(screen.getByRole('alert')).toHaveTextContent(strings.amountInvalid)
    expect(screen.getByRole('textbox', { name: strings.amountLabel }).closest('label')).toHaveClass('shake')
    expect(createOperation).not.toHaveBeenCalled()
  })

  it('asks to hold the button to save a huge amount', async () => {
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '50000000000')

    expect(screen.getByText(asRendered(strings.amountHuge(formatMoney('50000000000', 'KZT', 'ru'))))).toBeInTheDocument()
    await userEvent.click(saveButton(strings.holdToSave))
    expect(createOperation).not.toHaveBeenCalled()

    saveButton(strings.holdToSave).focus()
    await userEvent.keyboard('{Enter}')
    expect(createOperation).toHaveBeenCalled()
  })

  it('keeps the form and shows the server message when saving fails', async () => {
    vi.mocked(createOperation).mockRejectedValue(new ApiRequestError('bad', 400, { code: 'BAD_REQUEST', message: 'Операцию нельзя записать будущей датой.', details: {} }))
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '100')

    await userEvent.click(saveButton())

    expect(await screen.findByText('Операцию нельзя записать будущей датой.')).toBeInTheDocument()
    expect(amountInput()).toHaveValue('100')
    expect(onSaved).not.toHaveBeenCalled()
  })

  it('picks an older day in the calendar but not a future one', async () => {
    renderForm()
    await userEvent.click(await screen.findByRole('button', { name: strings.pickDate }))
    const calendar = screen.getByRole('dialog', { name: strings.calendarLabel })

    const days = within(calendar).getAllByRole('button').filter((button) => /^\d+$/.test(button.textContent ?? ''))
    const todayButton = days.find((button) => button.getAttribute('aria-current') === 'date')
    expect(days.slice(days.indexOf(todayButton!) + 1).every((button) => button.hasAttribute('disabled'))).toBe(true)

    await userEvent.click(within(calendar).getByRole('button', { name: strings.previousMonth }))
    await userEvent.click(within(calendar).getByRole('button', { name: '1' }))
    await userEvent.click(within(calendar).getByRole('button', { name: /^Выбрать/ }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: strings.pickDate })).toHaveAttribute('aria-pressed', 'true')
  })

  it('edits an existing operation in place', async () => {
    vi.mocked(replaceOperation).mockImplementation(async (_, id, input) => operation({ id, ...input }))
    renderForm(operation({ operationDate: today() }))
    const amount = await screen.findByRole('textbox', { name: strings.amountLabel })
    expect(amount).toHaveValue('3500')
    expect(screen.getByRole('textbox', { name: strings.commentLabel })).toHaveValue('Магнум')

    await userEvent.clear(amount)
    await userEvent.type(amount, '4000')
    await userEvent.click(saveButton(strings.saveChanges))

    expect(replaceOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, 'op-1', expect.objectContaining({ amount: '4000', categoryId: 'cat-1', description: 'Магнум' }))
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ created: false }))
  })
})
