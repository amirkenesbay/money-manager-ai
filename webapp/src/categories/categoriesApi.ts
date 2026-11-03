import { apiRequest } from '../api/client'
import type { Category, CategoryType } from '../api/types'

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

export const setCategoryLimit = (groupId: string, categoryId: string, amount: string | null): Promise<Category> =>
  apiRequest<Category>(`${categoriesPath(groupId)}/${categoryId}/limit`, { method: 'PUT', body: JSON.stringify({ amount }) })

export const deleteCategory = (groupId: string, categoryId: string): Promise<void> =>
  apiRequest<void>(`${categoriesPath(groupId)}/${categoryId}`, { method: 'DELETE' })
