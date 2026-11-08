import { describe, expect, it } from 'vitest'
import { EMOJI_CATALOG, searchEmoji, suggestEmoji } from './emojiCatalog'

describe('emoji catalog', () => {
  it('suggests an icon from a category name in any app language', () => {
    expect(suggestEmoji('Кофе')).toBe('☕')
    expect(suggestEmoji('Такси')).toBe('🚕')
    expect(suggestEmoji('Аренда квартиры')).toBe('🏠')
    expect(suggestEmoji('Coffee')).toBe('☕')
    expect(suggestEmoji('Жалақы')).toBe('💰')
  })

  it('suggests nothing for a name it does not know', () => {
    expect(suggestEmoji('Хобби Х')).toBeNull()
    expect(suggestEmoji('')).toBeNull()
  })

  it('finds icons by a word and shows everything for an empty query', () => {
    expect(searchEmoji('такс').map((item) => item.emoji)).toContain('🚕')
    expect(searchEmoji('  ')).toHaveLength(EMOJI_CATALOG.length)
  })

  it('keeps every icon once', () => {
    expect(new Set(EMOJI_CATALOG.map((item) => item.emoji)).size).toBe(EMOJI_CATALOG.length)
  })
})
