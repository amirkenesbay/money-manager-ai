import { apiRequest } from '../api/client'
import type { Category, CategoryRule, CategoryType } from '../api/types'

const categoriesPath = (groupId: string) => `/groups/${groupId}/categories`

export interface CategoryInput {
  name: string
  icon: string | null
}

export const fetchCategories = (groupId: string, signal?: AbortSignal): Promise<Category[]> =>
  apiRequest<Category[]>(categoriesPath(groupId), {}, signal)

export const createCategory = (groupId: string, input: CategoryInput & { type: CategoryType }): Promise<Category> =>
  apiRequest<Category>(categoriesPath(groupId), { method: 'POST', body: JSON.stringify(input) })

export const updateCategory = (groupId: string, categoryId: string, input: CategoryInput): Promise<Category> =>
  apiRequest<Category>(`${categoriesPath(groupId)}/${categoryId}`, { method: 'PATCH', body: JSON.stringify(input) })

export const setCategoryLimit = (groupId: string, categoryId: string, amount: string | null, rollover: boolean): Promise<Category> =>
  apiRequest<Category>(`${categoriesPath(groupId)}/${categoryId}/limit`, { method: 'PUT', body: JSON.stringify({ amount, rollover }) })

export const deleteCategory = (groupId: string, categoryId: string): Promise<void> =>
  apiRequest<void>(`${categoriesPath(groupId)}/${categoryId}`, { method: 'DELETE' })

const rulesPath = (groupId: string) => `/groups/${groupId}/category-rules`

export const fetchCategoryRules = (groupId: string, signal?: AbortSignal): Promise<CategoryRule[]> =>
  apiRequest<CategoryRule[]>(rulesPath(groupId), {}, signal)

export const forgetCategoryRule = (groupId: string, ruleId: string): Promise<void> =>
  apiRequest<void>(`${rulesPath(groupId)}/${ruleId}`, { method: 'DELETE' })
