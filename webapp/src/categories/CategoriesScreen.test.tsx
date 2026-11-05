import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { strings } from '../strings'
import { category, group, me, PERSONAL_GROUP_ID } from '../test/fixtures'
import { createCategory, reorderCategories, deleteCategory, fetchCategories, fetchCategoryRules, forgetCategoryRule, updateCategory } from './categoriesApi'
import { CategoriesScreen } from './CategoriesScreen'

vi.mock('./categoriesApi', () => ({
  reorderCategories: vi.fn(),
  fetchCategories: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
  deleteCategory: vi.fn(),
  fetchCategoryRules: vi.fn(),
  forgetCategoryRule: vi.fn(),
}))

const FREE_LIMIT = 10
const notify = vi.fn()
const groceries = category()
const salary = category({ id: 'cat-2', name: 'Зарплата', icon: '💰', type: 'INCOME' })
const renderScreen = (shared = false) =>
  render(<CategoriesScreen me={me()} group={group(shared ? { type: 'SHARED', memberCount: 3 } : {})} notify={notify} onBack={vi.fn()} />)
const nameInput = () => screen.getByRole('textbox', { name: strings.nameLabel })

describe('CategoriesScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchCategories).mockResolvedValue([groceries, salary])
    vi.mocked(fetchCategoryRules).mockResolvedValue([])
  })

  it('shows the places remembered for the chosen type and forgets one', async () => {
    const user = userEvent.setup()
    const magnum = { id: 'rule-1', place: 'магнум', categoryId: groceries.id, categoryName: groceries.name, categoryIcon: groceries.icon }
    const bonus = { id: 'rule-2', place: 'премия', categoryId: salary.id, categoryName: salary.name, categoryIcon: salary.icon }
    vi.mocked(fetchCategoryRules).mockResolvedValueOnce([magnum, bonus]).mockResolvedValue([bonus])
    vi.mocked(forgetCategoryRule).mockResolvedValue(undefined)
    renderScreen()

    expect(await screen.findByText(strings.rememberedPlace('магнум'))).toBeInTheDocument()
    expect(screen.queryByText(strings.rememberedPlace('премия'))).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: strings.forgetPlace('магнум') }))

    expect(forgetCategoryRule).toHaveBeenCalledWith(PERSONAL_GROUP_ID, 'rule-1')
    expect(notify).toHaveBeenCalledWith({ title: strings.placeForgottenToast })
    expect(screen.queryByText(strings.rememberedPlace('магнум'))).not.toBeInTheDocument()
  })

  it('lists categories of the chosen type with the free counter', async () => {
    const user = userEvent.setup()
    renderScreen()

    expect(await screen.findByText('Продукты')).toBeInTheDocument()
    expect(screen.queryByText('Зарплата')).not.toBeInTheDocument()
    expect(screen.getByText(strings.limitCounter(1, FREE_LIMIT))).toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: strings.typeFilter('INCOME') }))
    expect(screen.getByText('Зарплата')).toBeInTheDocument()
  })

  it('creates a category and confirms it', async () => {
    const user = userEvent.setup()
    const cafe = category({ id: 'cat-3', name: 'Кафе', icon: '☕' })
    vi.mocked(createCategory).mockResolvedValue(cafe)
    renderScreen()

    await user.click(await screen.findByRole('button', { name: strings.addCategory }))
    await user.type(nameInput(), '  Кафе ')
    await user.click(screen.getByRole('button', { name: '☕' }))
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(createCategory).toHaveBeenCalledWith(PERSONAL_GROUP_ID, { name: 'Кафе', icon: '☕', type: 'EXPENSE' })
    expect(notify).toHaveBeenCalledWith({ title: strings.categoryCreatedToast, detail: '☕ Кафе' })
    expect(await screen.findByRole('button', { name: strings.addCategory })).toBeInTheDocument()
  })

  it('asks for a name before saving', async () => {
    const user = userEvent.setup()
    renderScreen()

    await user.click(await screen.findByRole('button', { name: strings.addCategory }))
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(screen.getByRole('alert')).toHaveTextContent(strings.nameRequired)
    expect(createCategory).not.toHaveBeenCalled()
  })

  it('shows the duplicate name under the field', async () => {
    const user = userEvent.setup()
    const message = 'Категория «Продукты» уже есть.'
    vi.mocked(createCategory).mockRejectedValue(new ApiRequestError(message, 409, { code: 'CATEGORY_DUPLICATE', message, details: {} }))
    renderScreen()

    await user.click(await screen.findByRole('button', { name: strings.addCategory }))
    await user.type(nameInput(), 'Продукты')
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
  })

  it('offers premium instead of a form at the free limit', async () => {
    const user = userEvent.setup()
    vi.mocked(fetchCategories).mockResolvedValue(Array.from({ length: FREE_LIMIT }, (_, index) => category({ id: `cat-${index}`, name: `Категория ${index}` })))
    renderScreen()

    await user.click(await screen.findByRole('button', { name: strings.addCategory }))

    expect(screen.getByText(strings.categoryLimitTitle)).toBeInTheDocument()
    expect(screen.getByText(strings.limitCounter(FREE_LIMIT, FREE_LIMIT))).toBeInTheDocument()
    expect(screen.getByRole('button', { name: strings.getPremium })).toBeInTheDocument()
  })

  it('renames a shared category and warns that members get a message', async () => {
    const user = userEvent.setup()
    vi.mocked(updateCategory).mockResolvedValue({ ...groceries, name: 'Еда' })
    renderScreen(true)

    await user.click(await screen.findByRole('button', { name: /^Продукты/ }))
    expect(screen.getByText(strings.sharedChangeNotice)).toBeInTheDocument()
    await user.clear(nameInput())
    await user.type(nameInput(), 'Еда')
    await user.click(screen.getByRole('button', { name: strings.saveChanges }))

    expect(updateCategory).toHaveBeenCalledWith(PERSONAL_GROUP_ID, groceries.id, { name: 'Еда', icon: '🛒' })
    expect(notify).toHaveBeenCalledWith({ title: strings.categorySavedToast, detail: '🛒 Еда' })
  })

  it('deletes a category after confirmation', async () => {
    const user = userEvent.setup()
    vi.mocked(deleteCategory).mockResolvedValue(undefined)
    renderScreen()

    await user.click(await screen.findByRole('button', { name: /^Продукты/ }))
    await user.click(screen.getByRole('button', { name: strings.deleteCategory }))
    await user.click(screen.getByRole('button', { name: strings.delete }))

    expect(deleteCategory).toHaveBeenCalledWith(PERSONAL_GROUP_ID, groceries.id)
    expect(notify).toHaveBeenCalledWith({ title: strings.categoryDeletedToast, detail: '🛒 Продукты' })
  })

  it('saves a new order when a category is dragged above another', async () => {
    const taxi = category({ id: 'cat-taxi', name: 'Такси', icon: '🚕' })
    vi.mocked(fetchCategories).mockResolvedValue([groceries, taxi, salary])
    vi.mocked(reorderCategories).mockResolvedValue([taxi, groceries, salary])
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
      const index = ['cat-1', 'cat-taxi'].indexOf(this.dataset.reorderId ?? '')
      return { top: index * 50, bottom: index * 50 + 50, left: 0, right: 300, width: 300, height: 50, x: 0, y: index * 50, toJSON: () => ({}) } as DOMRect
    })
    renderScreen()
    const handle = await screen.findByRole('button', { name: strings.moveCategory('Такси') })

    fireEvent.pointerDown(handle, { clientY: 75, pointerId: 1 })
    fireEvent.pointerMove(handle, { clientY: 20, pointerId: 1 })
    fireEvent.pointerUp(handle, { clientY: 20, pointerId: 1 })

    await waitFor(() => expect(reorderCategories).toHaveBeenCalledWith(group().id, ['cat-taxi', groceries.id, salary.id]))
    vi.restoreAllMocks()
  })
})
