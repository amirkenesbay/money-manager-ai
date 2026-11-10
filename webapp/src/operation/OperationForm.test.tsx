import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { formatMoney, toIsoDate } from '../format'
import { createCategory, fetchCategories } from '../categories/categoriesApi'
import { createOperation, fetchRate, fetchRecentTags, replaceOperation } from '../operations/operationsApi'
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
  fetchRate: vi.fn(),
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
const recordButton = () => screen.getByRole('button', { name: new RegExp(`^(${strings.recordAmount('').trim()}|${strings.save}|${strings.saveChanges})`) })
const typeAmount = async (text: string) => userEvent.type(await screen.findByRole('textbox', { name: strings.amountLabel }), text)
const pickCategory = async (name: RegExp) => {
  await userEvent.click(screen.getByRole('button', { name: new RegExp(`^${strings.categoryLabel}:`) }))
  await userEvent.click(within(screen.getByRole('dialog', { name: strings.categoryLabel })).getByRole('button', { name }))
}
const pickQuickDate = async (daysAgo: number) => {
  await userEvent.click(screen.getByRole('button', { name: new RegExp(`^${strings.dateLabel}:`) }))
  await userEvent.click(screen.getByRole('button', { name: strings.quickDate(daysAgo) }))
}
const openDetails = async () => userEvent.click(await screen.findByRole('button', { name: new RegExp(`^${strings.moreDetails}`) }))
const closeDetails = async () => userEvent.click(within(screen.getByRole('dialog', { name: strings.moreDetails })).getByRole('button', { name: strings.done }))

describe('OperationForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    localStorage.clear()
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
    await typeAmount('5 000')
    await pickCategory(/Такси/)
    await pickQuickDate(1)
    await openDetails()
    await userEvent.type(screen.getByRole('textbox', { name: strings.commentLabel }), '  до работы ')
    await closeDetails()

    await userEvent.click(recordButton())

    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, {
      type: 'EXPENSE', amount: '5000', categoryId: 'cat-2', operationDate: daysAgo(1), description: 'до работы',
    })
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ created: true }))
  })

  it('adds up prices on the keypad and records the total', async () => {
    renderForm()
    await screen.findByRole('textbox', { name: strings.amountLabel })
    const pad = screen.getByRole('group', { name: strings.amountPadLabel })
    for (const key of ['1', '1', '2', '0', '+', '3', '5', '0']) await userEvent.click(within(pad).getByRole('button', { name: key }))

    expect(amountInput()).toHaveValue('1 120 + 350')
    expect(screen.getByText(asRendered(`= ${formatMoney('1470', 'KZT', 'ru')}`))).toBeInTheDocument()
    await userEvent.click(recordButton())

    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ amount: '1470' }))
  })

  it('shows the amount on the record button', async () => {
    renderForm()
    await typeAmount('1120')

    expect(recordButton()).toHaveTextContent(asRendered(strings.recordAmount(formatMoney('1120', 'KZT', 'ru'))))
  })

  it('creates a missing category with a matching icon right from the picker and records into it', async () => {
    vi.mocked(createCategory).mockImplementation(async (_, { name, type }) => category({ id: 'cat-new', name, type }))
    renderForm()
    await typeAmount('1200')
    await userEvent.click(screen.getByRole('button', { name: new RegExp(`^${strings.categoryLabel}:`) }))
    await userEvent.click(screen.getByRole('button', { name: strings.addCategory }))
    const sheet = screen.getByRole('dialog', { name: strings.newCategoryTitle })
    await userEvent.type(within(sheet).getByRole('textbox', { name: strings.nameLabel }), 'Кафе')
    await userEvent.click(within(sheet).getByRole('button', { name: strings.save }))

    expect(createCategory).toHaveBeenCalledWith(PERSONAL_GROUP_ID, { name: 'Кафе', icon: '🍽️', type: 'EXPENSE' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: new RegExp(`^${strings.categoryLabel}: Кафе`) })).toBeInTheDocument()

    await userEvent.click(recordButton())
    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ categoryId: 'cat-new' }))
  })

  it('records who paid and for whom in a group that splits expenses', async () => {
    const me = 42
    vi.mocked(fetchMembers).mockResolvedValue([{ userId: me, name: 'Dev', isOwner: true }, { userId: 43, name: 'Аня', isOwner: false }])
    render(<OperationForm group={group({ id: 'family', type: 'SHARED', splitExpenses: true })} userId={me} language="ru" operation={null} onBack={vi.fn()} onSaved={onSaved} onDeleted={vi.fn()} />)
    await typeAmount('3000')
    await userEvent.click(await screen.findByRole('button', { name: new RegExp(`${strings.paidForLabel}: ${strings.forMyself}`) }))
    await userEvent.click(screen.getByRole('button', { name: /Аня/, pressed: false }))
    await userEvent.click(screen.getByRole('button', { name: new RegExp(strings.paidByLabel) }))

    expect(screen.getByRole('status')).toHaveTextContent(asRendered(strings.shareMine(formatMoney('1500', 'KZT', 'ru'))))
    await userEvent.click(screen.getByRole('button', { name: strings.done }))
    await userEvent.click(recordButton())

    expect(createOperation).toHaveBeenCalledWith('family', expect.objectContaining({ payerId: 43, participantIds: [me, 43] }))
  })

  it('adds a typed tag and a recent one to the description', async () => {
    renderForm()
    await typeAmount('1500')
    await pickCategory(/Такси/)
    await openDetails()
    await userEvent.type(screen.getByRole('textbox', { name: strings.commentLabel }), 'кофе')
    await userEvent.click(screen.getByRole('button', { name: strings.addTag }))
    await userEvent.type(screen.getByRole('textbox', { name: strings.newTagLabel }), 'Новый год{Enter}')
    await userEvent.click(await screen.findByRole('button', { name: strings.suggestTag('отпуск') }))
    await closeDetails()

    expect(screen.getByRole('button', { name: new RegExp(`^${strings.moreDetails}`) })).toHaveTextContent(strings.moreDetailsCount(3))
    await userEvent.click(recordButton())

    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ description: 'кофе #новый_год #отпуск' }))
  })

  it('shows the tags of an operation apart from its comment and lets the user remove one', async () => {
    vi.mocked(replaceOperation).mockResolvedValue(operation())
    renderForm(operation({ description: 'Магнум #отпуск #море' }))

    await openDetails()
    expect(screen.getByRole('textbox', { name: strings.commentLabel })).toHaveValue('Магнум')
    await userEvent.click(screen.getByRole('button', { name: strings.removeTag('море') }))
    await closeDetails()
    await userEvent.click(recordButton())

    expect(replaceOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.any(String), expect.objectContaining({ description: 'Магнум #отпуск' }))
  })

  it('makes a new operation repeat every month', async () => {
    const saved = operation({ id: 'op-new', amount: '3500', categoryId: 'cat-2', operationDate: today(), description: 'Netflix' })
    vi.mocked(createOperation).mockResolvedValue(saved)
    vi.mocked(createRecurring).mockResolvedValue({} as Awaited<ReturnType<typeof createRecurring>>)
    renderForm()
    await typeAmount('3500')
    await pickCategory(/Такси/)
    await openDetails()
    await userEvent.click(screen.getByRole('radio', { name: strings.scheduleEveryMonth }))
    await closeDetails()

    await userEvent.click(recordButton())

    expect(createRecurring).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ unit: 'MONTH', interval: 1, startDate: today(), firstRecorded: true, mode: 'AUTO' }))
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ note: strings.repeatSavedToast(strings.scheduleEveryMonth) }))
  })

  it('offers only categories of the chosen type', async () => {
    renderForm()
    await screen.findByRole('textbox', { name: strings.amountLabel })

    await userEvent.click(screen.getByRole('radio', { name: strings.operationType('INCOME') }))
    await userEvent.click(screen.getByRole('button', { name: new RegExp(`^${strings.categoryLabel}: Зарплата`) }))

    const sheet = screen.getByRole('dialog', { name: strings.categoryLabel })
    expect(within(sheet).getByRole('button', { name: /Зарплата/ })).toHaveAttribute('aria-pressed', 'true')
    expect(within(sheet).queryByRole('button', { name: /Такси/ })).not.toBeInTheDocument()
  })

  it('blocks saving a zero amount and explains why', async () => {
    renderForm()
    await typeAmount('0')

    expect(screen.getByRole('alert')).toHaveTextContent(strings.amountInvalid)
    await userEvent.click(recordButton())

    expect(createOperation).not.toHaveBeenCalled()
  })

  it('shakes the amount when saving without one', async () => {
    renderForm()
    await userEvent.click(await screen.findByRole('button', { name: strings.save }))

    expect(screen.getByRole('alert')).toHaveTextContent(strings.amountInvalid)
    expect(amountInput().closest('label')).toHaveClass('shake')
    expect(createOperation).not.toHaveBeenCalled()
  })

  it('asks to hold the button to save a huge amount', async () => {
    renderForm()
    await typeAmount('50000000000')

    expect(screen.getByText(asRendered(strings.amountHuge(formatMoney('50000000000', 'KZT', 'ru'))))).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.holdToSave }))
    expect(createOperation).not.toHaveBeenCalled()

    screen.getByRole('button', { name: strings.holdToSave }).focus()
    await userEvent.keyboard('{Enter}')
    expect(createOperation).toHaveBeenCalled()
  })

  it('keeps the form and shows the server message when saving fails', async () => {
    vi.mocked(createOperation).mockRejectedValue(new ApiRequestError('bad', 400, { code: 'BAD_REQUEST', message: 'Операцию нельзя записать будущей датой.', details: {} }))
    renderForm()
    await typeAmount('100')

    await userEvent.click(recordButton())

    expect(await screen.findByText('Операцию нельзя записать будущей датой.')).toBeInTheDocument()
    expect(amountInput()).toHaveValue('100')
    expect(onSaved).not.toHaveBeenCalled()
  })

  it('picks an older day in the calendar but not a future one', async () => {
    renderForm()
    await screen.findByRole('textbox', { name: strings.amountLabel })
    await userEvent.click(screen.getByRole('button', { name: new RegExp(`^${strings.dateLabel}:`) }))
    await userEvent.click(screen.getByRole('button', { name: strings.pickDate }))
    const calendar = screen.getByRole('dialog', { name: strings.calendarLabel })

    const days = within(calendar).getAllByRole('button').filter((button) => /^\d+$/.test(button.textContent ?? ''))
    const todayButton = days.find((button) => button.getAttribute('aria-current') === 'date')
    expect(days.slice(days.indexOf(todayButton!) + 1).every((button) => button.hasAttribute('disabled'))).toBe(true)

    await userEvent.click(within(calendar).getByRole('button', { name: strings.previousMonth }))
    await userEvent.click(within(calendar).getByRole('button', { name: '1' }))
    await userEvent.click(within(calendar).getByRole('button', { name: /^Выбрать/ }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: new RegExp(`^${strings.dateLabel}:`) })).not.toHaveTextContent(strings.quickDate(0))
  })

  it('edits an existing operation in place', async () => {
    vi.mocked(replaceOperation).mockImplementation(async (_, id, input) => operation({ id, ...input }))
    renderForm(operation({ operationDate: today() }))
    const amount = await screen.findByRole('textbox', { name: strings.amountLabel })
    expect(amount).toHaveValue('3 500')

    await userEvent.clear(amount)
    await userEvent.type(amount, '4000')
    await userEvent.click(recordButton())

    expect(replaceOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, 'op-1', expect.objectContaining({ amount: '4000', categoryId: 'cat-1', description: 'Магнум' }))
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ created: false }))
  })
  describe('in another currency', () => {
    const pickEuro = async () => {
      await userEvent.click(await screen.findByRole('button', { name: `${strings.currencyFieldLabel}: KZT` }))
      await userEvent.click(within(screen.getByRole('dialog', { name: strings.currencyFieldLabel })).getByRole('radio', { name: `${strings.currencyName('EUR')} EUR` }))
    }

    beforeEach(() => {
      vi.mocked(fetchRate).mockResolvedValue({ from: 'EUR', to: 'KZT', rate: '521.6', date: today() })
    })

    it('records the tenge amount by the bank rate and keeps the euros', async () => {
      renderForm()
      await pickEuro()
      await typeAmount('100')

      expect(await screen.findByText(asRendered(`≈ ${formatMoney('52160', 'KZT', 'ru')}`))).toBeInTheDocument()
      await userEvent.click(recordButton())

      expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ amount: '52160', original: { amount: '100', currency: 'EUR' } }))
    })

    it('records what the card was actually charged', async () => {
      renderForm()
      await pickEuro()
      await typeAmount('100')
      await userEvent.click(await screen.findByRole('button', { name: /1 € = 521,6/ }))
      const total = screen.getByRole('textbox', { name: strings.rateTotal('₸') })
      await userEvent.clear(total)
      await userEvent.type(total, '53000')
      await userEvent.click(within(screen.getByRole('dialog', { name: strings.rateTitle })).getByRole('button', { name: strings.done }))

      await userEvent.click(recordButton())

      expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ amount: '53000', original: { amount: '100', currency: 'EUR' } }))
    })

    it('asks for a rate when the bank has none', async () => {
      vi.mocked(fetchRate).mockRejectedValue(new Error('no rate'))
      renderForm()
      await pickEuro()
      await typeAmount('100')

      await userEvent.click(await screen.findByRole('button', { name: strings.rateMissing }))

      expect(screen.getByRole('dialog', { name: strings.rateTitle })).toBeInTheDocument()
    })

    it('opens a foreign expense in the currency it was typed in', async () => {
      renderForm(operation({ amount: '52160', original: { amount: '100', currency: 'EUR' } }))

      expect(await screen.findByRole('button', { name: `${strings.currencyFieldLabel}: EUR` })).toBeInTheDocument()
      expect(amountInput()).toHaveValue('100')
      expect(fetchRate).not.toHaveBeenCalled()
    })
  })
})
