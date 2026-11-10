import type { Category, CategoryType, Operation, VoiceOperation } from '../api/types'
import { createCategory } from '../categories/categoriesApi'
import { parseAmount } from '../operation/amount'
import { createOperation } from '../operations/operationsApi'

export interface Draft extends VoiceOperation {
  key: string
  categoryCorrected: boolean
}

export const toDrafts = (operations: VoiceOperation[]): Draft[] =>
  operations.map((operation, index) => ({ ...operation, key: `draft-${index}`, categoryCorrected: false }))

const sameOperation = (left: Draft, right: Draft): boolean =>
  left.type === right.type && Number(left.amount) === Number(right.amount) && left.categoryName === right.categoryName
    && left.operationDate === right.operationDate && (left.description ?? '') === (right.description ?? '')

export const repeatedKeys = (drafts: Draft[]): Set<string> =>
  new Set(drafts.filter((draft, index) => drafts.slice(0, index).some((earlier) => sameOperation(earlier, draft))).map((draft) => draft.key))

export const draftReady = (draft: Draft): boolean =>
  parseAmount(draft.amount) !== null && (draft.categoryId !== null || (draft.isNewCategory && draft.categoryName !== null))

export function withType(draft: Draft, type: CategoryType): Draft {
  if (draft.type === type) return draft
  return { ...draft, type, categoryId: null, categoryName: null, categoryIcon: null, isNewCategory: false }
}

export const withCategory = (draft: Draft, category: Category): Draft =>
  ({ ...draft, categoryId: category.id, categoryName: category.name, categoryIcon: category.icon, isNewCategory: false, categoryCorrected: true })

const categoryKey = (type: CategoryType, name: string) => `${type}:${name.toLowerCase()}`

export async function saveDraft(groupId: string, draft: Draft, created: Map<string, Category>): Promise<Operation> {
  const categoryId = draft.categoryId ?? (await ensureCategory(groupId, draft, created)).id
  return createOperation(groupId, {
    type: draft.type,
    amount: parseAmount(draft.amount) ?? draft.amount,
    categoryId,
    operationDate: draft.operationDate,
    description: draft.description,
    categoryCorrected: draft.categoryCorrected,
    payerId: draft.payerId,
    participantIds: draft.participantIds,
    original: draft.original,
  })
}

async function ensureCategory(groupId: string, draft: Draft, created: Map<string, Category>): Promise<Category> {
  const key = categoryKey(draft.type, draft.categoryName ?? '')
  const existing = created.get(key)
  if (existing) return existing
  const category = await createCategory(groupId, { name: draft.categoryName ?? '', icon: draft.categoryIcon, type: draft.type })
  created.set(key, category)
  return category
}
