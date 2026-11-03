import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import type { Budget } from '../api/types'
import { CategoryForm } from '../categories/CategoryForm'
import { createCategory, setCategoryLimit, updateCategory } from '../categories/categoriesApi'
import { formatMoney } from '../format'
import { strings } from '../strings'
import { asRendered } from '../test/text'
import { category, group } from '../test/fixtures'
import { BudgetNote } from './BudgetNote'
import { fetchBudgets } from './budgetsApi'
import { BudgetsSummary } from './BudgetsSummary'
import { budgetTone } from './budgetTone'

vi.mock('./budgetsApi', () => ({ fetchBudgets: vi.fn() }))
vi.mock('../categories/categoriesApi', () => ({ createCategory: vi.fn(), updateCategory: vi.fn(), deleteCategory: vi.fn(), setCategoryLimit: vi.fn() }))

const LANGUAGE = 'ru'
const budget = (overrides: Partial<Budget> = {}): Budget => ({
  categoryId: 'cat-1', icon: '🛒', name: 'Продукты', limit: '100000', spent: '62000', remaining: '38000', percent: 62,
  from: '2026-10-01', to: '2026-10-31', ...overrides,
})
const taxi = budget({ categoryId: 'cat-2', icon: '🚕', name: 'Такси', limit: '20000', spent: '17000', remaining: '3000', percent: 85 })
const fun = budget({ categoryId: 'cat-3', icon: '🎬', name: 'Развлечения', limit: '30000', spent: '31200', remaining: '-1200', percent: 104 })

const renderSummary = () => render(<BudgetsSummary groupId={group().id} currency="KZT" language={LANGUAGE} onOpenAll={vi.fn()} />)

describe('budget tone', () => {
  it('turns orange at 80 percent and red past the limit', () => {
    expect(budgetTone(79)).toBe('ok')
    expect(budgetTone(80)).toBe('near')
    expect(budgetTone(100)).toBe('over')
  })
})

describe('BudgetsSummary', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows only the budgets that need attention', async () => {
    vi.mocked(fetchBudgets).mockResolvedValue([fun, taxi, budget()])
    renderSummary()

    expect(await screen.findByText(asRendered(strings.budgetLeft(formatMoney('3000', 'KZT', LANGUAGE))))).toBeInTheDocument()
    expect(screen.getByText(asRendered(strings.budgetOver(formatMoney('1200', 'KZT', LANGUAGE))))).toBeInTheDocument()
    expect(screen.queryByText('🛒 Продукты')).not.toBeInTheDocument()
    expect(screen.getByText(strings.budgetsMoreFine(1))).toBeInTheDocument()
  })

  it('folds into one line when every budget is fine', async () => {
    vi.mocked(fetchBudgets).mockResolvedValue([budget()])
    renderSummary()

    expect(await screen.findByText(strings.budgetsCalm)).toBeInTheDocument()
  })

  it('stays away when the group has no budgets', async () => {
    vi.mocked(fetchBudgets).mockResolvedValue([])
    const { container } = renderSummary()

    await vi.waitFor(() => expect(fetchBudgets).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })
})

describe('BudgetNote', () => {
  beforeEach(() => vi.resetAllMocks())

  it('tells what is left after the expense being typed', async () => {
    vi.mocked(fetchBudgets).mockResolvedValue([taxi])
    render(<BudgetNote groupId={group().id} category={category({ id: 'cat-2', icon: '🚕', name: 'Такси' })} amount="1000" editing={null} currency="KZT" language={LANGUAGE} />)

    expect(await screen.findByText(asRendered(strings.budgetAfterOperation('🚕 Такси', formatMoney('2000', 'KZT', LANGUAGE), formatMoney('20000', 'KZT', LANGUAGE))))).toBeInTheDocument()
  })

  it('warns that the expense goes over the limit', async () => {
    vi.mocked(fetchBudgets).mockResolvedValue([taxi])
    render(<BudgetNote groupId={group().id} category={category({ id: 'cat-2', icon: '🚕', name: 'Такси' })} amount="5000" editing={null} currency="KZT" language={LANGUAGE} />)

    expect(await screen.findByText(asRendered(strings.budgetAfterOperationOver('🚕 Такси', formatMoney('2000', 'KZT', LANGUAGE))))).toBeInTheDocument()
  })
})

describe('CategoryForm limit', () => {
  beforeEach(() => vi.resetAllMocks())

  const renderForm = (onSaved = vi.fn(), onBudgetLimit = vi.fn()) => render(
    <CategoryForm group={group()} language={LANGUAGE} type="EXPENSE" category={category()} onBack={vi.fn()} onSaved={onSaved} onDeleted={vi.fn()} onLimit={vi.fn()} onBudgetLimit={onBudgetLimit} />,
  )

  it('saves a monthly limit together with the category', async () => {
    vi.mocked(updateCategory).mockResolvedValue(category())
    vi.mocked(setCategoryLimit).mockResolvedValue(category({ monthlyLimit: '100000' }))
    const onSaved = vi.fn()
    renderForm(onSaved)

    await userEvent.type(screen.getByRole('textbox', { name: strings.budgetLimitLabel }), '100 000')
    await userEvent.click(screen.getByRole('button', { name: strings.saveChanges }))

    expect(setCategoryLimit).toHaveBeenCalledWith(group().id, 'cat-1', '100000')
    expect(onSaved).toHaveBeenCalledWith(category({ monthlyLimit: '100000' }), false)
    expect(createCategory).not.toHaveBeenCalled()
  })

  it('shows the free budget limit instead of an error', async () => {
    vi.mocked(updateCategory).mockResolvedValue(category())
    vi.mocked(setCategoryLimit).mockRejectedValue(new ApiRequestError('limit', 422, { code: 'BUDGET_LIMIT_REACHED', message: 'limit', details: { limit: 3 } }))
    const onBudgetLimit = vi.fn()
    renderForm(vi.fn(), onBudgetLimit)

    await userEvent.type(screen.getByRole('textbox', { name: strings.budgetLimitLabel }), '5000')
    await userEvent.click(screen.getByRole('button', { name: strings.saveChanges }))

    expect(onBudgetLimit).toHaveBeenCalled()
  })

  it('hides the limit for income categories', () => {
    render(<CategoryForm group={group()} language={LANGUAGE} type="INCOME" category={null} onBack={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} onLimit={vi.fn()} onBudgetLimit={vi.fn()} />)

    expect(screen.queryByRole('textbox', { name: strings.budgetLimitLabel })).not.toBeInTheDocument()
  })
})
