import type { CategoryType } from '../api/types'

export const CATEGORY_TYPES: CategoryType[] = ['EXPENSE', 'INCOME']

export const MAX_CATEGORY_NAME_LENGTH = 50

export const ICON_PRESETS: Record<CategoryType, string[]> = {
  EXPENSE: ['🛒', '🍔', '☕', '🚕', '🚗', '🏠', '💊', '👕', '📚', '🎬', '✈️', '📱', '🐾', '🎁', '💡', '📦'],
  INCOME: ['💰', '💼', '🎁', '📈', '🏦', '💵', '🤝', '📦'],
}
