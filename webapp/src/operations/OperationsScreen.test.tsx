import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { formatSignedMoney } from '../format'
import { strings } from '../strings'
import { asRendered } from '../test/text'
import { openExternalLink } from '../telegram/telegramEnv'
import { category, group, me, operation, operationsPage, PERSONAL_GROUP_ID, SUBSCRIBE_URL } from '../test/fixtures'
import { fetchCategories } from '../categories/categoriesApi'
import { fetchOperations } from './operationsApi'
import { OperationsScreen } from './OperationsScreen'
import { periodRange } from './periods'

vi.mock('./operationsApi', () => ({
  OPERATIONS_PAGE_SIZE: 20,
  NO_FILTERS: { from: null, to: null, type: null, categoryId: null },
  fetchOperations: vi.fn(),
}))
vi.mock('../categories/categoriesApi', () => ({ fetchCategories: vi.fn() }))
vi.mock('../telegram/telegramEnv', () => ({ openExternalLink: vi.fn(), rawInitData: () => undefined }))

const PAGE_SIZE = 20
const firstPage = Array.from({ length: PAGE_SIZE }, (_, index) => operation({ id: `op-${index}`, categoryName: `Покупка ${index}` }))
const premiumRequired = new ApiRequestError('limit', 422, { code: 'PREMIUM_REQUIRED', message: 'limit', details: { historyDaysBack: 30 } })

const onAddOperation = vi.fn()
const onSelectOperation = vi.fn()
const renderScreen = () => render(<OperationsScreen me={me()} group={group()} onAddOperation={onAddOperation} onSelectOperation={onSelectOperation} />)
const lastQuery = () => vi.mocked(fetchOperations).mock.lastCall?.[1]

describe('OperationsScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchOperations).mockResolvedValue(operationsPage([operation()]))
    vi.mocked(fetchCategories).mockResolvedValue([
      category(),
      category({ id: 'cat-2', name: 'Такси', icon: '🚕' }),
      category({ id: 'cat-3', name: 'Зарплата', icon: '💰', type: 'INCOME' }),
    ])
  })

  it('starts with the last 30 days of all operation types', async () => {
    renderScreen()

    expect(await screen.findByText('Продукты')).toBeInTheDocument()
    expect(fetchOperations).toHaveBeenCalledWith(
      PERSONAL_GROUP_ID,
      { ...periodRange('last30Days', new Date()), type: null, categoryId: null },
      0,
      expect.anything(),
    )
  })

  it('filters by type and offers only categories of that type', async () => {
    renderScreen()
    await screen.findByText('Продукты')

    await userEvent.click(screen.getByRole('radio', { name: strings.typeFilter('INCOME') }))

    expect(lastQuery()).toMatchObject({ type: 'INCOME', categoryId: null })
    const categorySelect = await screen.findByRole('combobox', { name: strings.categoryFilterLabel })
    expect(screen.getAllByRole('option').map((option) => option.textContent)).not.toContain('🚕 Такси')

    await userEvent.selectOptions(categorySelect, 'cat-3')

    expect(lastQuery()).toMatchObject({ type: 'INCOME', categoryId: 'cat-3' })
  })

  it('loads the next page and appends it to the list', async () => {
    vi.mocked(fetchOperations)
      .mockResolvedValueOnce(operationsPage(firstPage, PAGE_SIZE + 1))
      .mockResolvedValueOnce(operationsPage([operation({ id: 'op-last', categoryName: 'Последняя' })], PAGE_SIZE + 1, 1))
    renderScreen()

    await userEvent.click(await screen.findByRole('button', { name: strings.loadMore }))

    expect(await screen.findByText('Последняя')).toBeInTheDocument()
    expect(screen.getByText('Покупка 0')).toBeInTheDocument()
    expect(vi.mocked(fetchOperations).mock.lastCall?.[2]).toBe(1)
    expect(screen.queryByRole('button', { name: strings.loadMore })).not.toBeInTheDocument()
  })

  it('drops loaded pages when the filter changes', async () => {
    vi.mocked(fetchOperations)
      .mockResolvedValueOnce(operationsPage(firstPage, PAGE_SIZE + 1))
      .mockResolvedValueOnce(operationsPage([operation({ id: 'op-last', categoryName: 'Последняя' })], PAGE_SIZE + 1, 1))
      .mockResolvedValueOnce(operationsPage([operation({ id: 'op-income', type: 'INCOME', categoryName: 'Зарплата' })]))
    renderScreen()
    await userEvent.click(await screen.findByRole('button', { name: strings.loadMore }))
    await screen.findByText('Последняя')

    await userEvent.click(screen.getByRole('radio', { name: strings.typeFilter('INCOME') }))

    expect(await screen.findByText('Зарплата', { selector: '.row-title' })).toBeInTheDocument()
    expect(screen.queryByText('Последняя')).not.toBeInTheDocument()
    expect(screen.queryByText('Покупка 0')).not.toBeInTheDocument()
  })

  it('offers Premium instead of an empty list when the period is beyond the free history', async () => {
    renderScreen()
    await screen.findByText('Продукты')
    vi.mocked(fetchOperations).mockRejectedValue(premiumRequired)

    await userEvent.selectOptions(screen.getByRole('combobox', { name: strings.periodFilterLabel }), 'lastMonth')

    await userEvent.click(await screen.findByRole('button', { name: new RegExp(strings.premiumHistoryTitle(30)) }))
    expect(openExternalLink).toHaveBeenCalledWith(SUBSCRIBE_URL)
  })

  it('offers Premium for the whole history without asking the server on Free', async () => {
    renderScreen()
    await screen.findByText('Продукты')
    vi.mocked(fetchOperations).mockClear()

    await userEvent.selectOptions(screen.getByRole('combobox', { name: strings.periodFilterLabel }), 'allTime')

    expect(await screen.findByText(strings.premiumHistoryTitle(30))).toBeInTheDocument()
    expect(fetchOperations).not.toHaveBeenCalled()
  })

  it('explains an empty result and resets the filters', async () => {
    vi.mocked(fetchOperations).mockResolvedValue(operationsPage([]))
    renderScreen()
    await userEvent.click(await screen.findByRole('radio', { name: strings.typeFilter('INCOME') }))

    expect(await screen.findByText(strings.operationsEmptyTitle)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.resetFilters }))

    expect(lastQuery()).toMatchObject({ ...periodRange('last30Days', new Date()), type: null, categoryId: null })
    expect(screen.getByRole('radio', { name: strings.typeFilter(null) })).toBeChecked()
  })

  it('groups operations by day with the day total', async () => {
    vi.mocked(fetchOperations).mockResolvedValue(operationsPage([
      operation({ id: 'op-1', amount: '3500' }),
      operation({ id: 'op-2', amount: '1500', categoryName: 'Такси' }),
      operation({ id: 'op-3', operationDate: '2026-10-11', type: 'INCOME', amount: '20000', categoryName: 'Зарплата' }),
    ]))

    renderScreen()

    const day = await screen.findByRole('region', { name: '12 октября' })
    expect(within(day).getAllByRole('listitem')).toHaveLength(2)
    expect(within(day).getByText(asRendered(formatSignedMoney('EXPENSE', '5000', 'KZT', 'ru')))).toBeInTheDocument()
    expect(screen.getByRole('region', { name: '11 октября' })).toBeInTheDocument()
  })

  it('opens the new operation form and the tapped operation', async () => {
    renderScreen()

    await userEvent.click(screen.getByRole('button', { name: strings.addOperation }))
    await userEvent.click(await screen.findByRole('button', { name: /Продукты/ }))

    expect(onAddOperation).toHaveBeenCalled()
    expect(onSelectOperation).toHaveBeenCalledWith(operation())
  })
})
