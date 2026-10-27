import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { formatMoney, toIsoDate } from '../format'
import { createOperation, fetchCategories, replaceOperation } from '../operations/operationsApi'
import { strings } from '../strings'
import { category, group, operation, PERSONAL_GROUP_ID } from '../test/fixtures'
import { asRendered } from '../test/text'
import { OperationForm } from './OperationForm'

vi.mock('../operations/operationsApi', () => ({
  fetchCategories: vi.fn(),
  createOperation: vi.fn(),
  replaceOperation: vi.fn(),
  deleteOperation: vi.fn(),
}))

const onSaved = vi.fn()
const today = () => toIsoDate(new Date())
const daysAgo = (days: number) => {
  const now = new Date()
  return toIsoDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() - days))
}
const renderForm = (editing: ReturnType<typeof operation> | null = null) =>
  render(<OperationForm group={group()} language="ru" operation={editing} onBack={vi.fn()} onSaved={onSaved} onDeleted={vi.fn()} />)
const amountInput = () => screen.getByRole('textbox', { name: strings.amountLabel })
const saveButton = (name = strings.save) => screen.getByRole('button', { name })

describe('OperationForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
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
    expect(saveButton()).toBeDisabled()
  })

  it('asks to double-check a huge amount', async () => {
    renderForm()
    await userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), '50000000000')

    expect(screen.getByText(asRendered(strings.amountHuge(formatMoney('50000000000', 'KZT', 'ru'))))).toBeInTheDocument()
    expect(saveButton(strings.saveHuge)).toBeEnabled()
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
