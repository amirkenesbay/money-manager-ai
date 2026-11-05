import type { Category, CategoryChoice, ImportCategory, ImportPreview, SkipReason } from '../api/types'
import { strings } from '../strings'

const NAMED_SOURCES = ['ZENMONEY', 'COINKEEPER', 'MONEY_MANAGER'] as const
const LIST_SEPARATOR = ', '
const PART_SEPARATOR = ' · '

export type Choices = Record<string, CategoryChoice>

export const defaultChoices = (preview: ImportPreview): Choices =>
  Object.fromEntries(preview.categories.map((category) => [category.key, defaultChoice(category)]))

const defaultChoice = (category: ImportCategory): CategoryChoice =>
  category.existingId ? { action: 'EXISTING', categoryId: category.existingId } : { action: 'CREATE' }

export const importedCount = (preview: ImportPreview, choices: Choices): number =>
  preview.categories.filter((category) => choices[category.key]?.action !== 'SKIP').reduce((total, category) => total + category.count, 0)

export function choiceLabel(category: ImportCategory, choice: CategoryChoice | undefined, categories: Category[]): string {
  if (!choice || choice.action === 'CREATE') return strings.importNew
  if (choice.action === 'SKIP') return strings.importSkip
  if (choice.categoryId === category.existingId) return strings.importExists
  return `→ ${categories.find((candidate) => candidate.id === choice.categoryId)?.name ?? ''}`
}

export function recognisedLabel(preview: ImportPreview): string {
  const named = NAMED_SOURCES.find((source) => source === preview.source)
  if (named) return strings.importLooksLike(strings.importApps[named])
  const roles = preview.columns.flatMap((column) => (column.role ? [strings.importRoles[column.role].toLowerCase()] : []))
  return strings.importColumnsFound(roles.join(LIST_SEPARATOR))
}

export function skippedLabel(skipped: Partial<Record<SkipReason, number>>): string | null {
  const parts = (Object.entries(skipped) as [SkipReason, number][]).filter(([, count]) => count > 0).map(([reason, count]) => `${strings.importSkipReasons[reason]} ${count}`)
  return parts.length > 0 ? strings.importSkipped(parts.join(PART_SEPARATOR)) : null
}
