import { describe, expect, it } from 'vitest'
import { normalizeTag, tagsOf, withoutTags, withTags } from './tags'

describe('tags', () => {
  it('splits a description into text and tags', () => {
    expect(tagsOf('Кофе #Отпуск у моря #отпуск #2026')).toEqual(['отпуск', '2026'])
    expect(withoutTags('Кофе #отпуск у моря')).toBe('Кофе у моря')
    expect(tagsOf(null)).toEqual([])
  })

  it('turns any input into one tag word', () => {
    expect(normalizeTag('  #Новый год! ')).toBe('новый_год')
    expect(normalizeTag('#')).toBe('')
  })

  it('joins text and tags back into a description', () => {
    expect(withTags(' Кофе ', ['отпуск', 'море'])).toBe('Кофе #отпуск #море')
    expect(withTags('', [])).toBeNull()
  })
})
