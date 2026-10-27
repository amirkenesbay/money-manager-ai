import { afterEach, describe, expect, it } from 'vitest'
import { applyLanguage, dictionaries, strings } from '../strings'
import { LANGUAGES } from './languages'

const shapeOf = (dictionary: object) => Object.entries(dictionary).map(([key, value]) => `${key}:${typeof value}`).sort()

describe('dictionaries', () => {
  afterEach(() => {
    applyLanguage('ru')
  })

  it.each(LANGUAGES)('has every key of the Russian dictionary in %s', (language) => {
    expect(shapeOf(dictionaries[language])).toEqual(shapeOf(dictionaries.ru))
  })

  it.each(LANGUAGES)('has no empty texts in %s', (language) => {
    const empty = Object.entries(dictionaries[language]).filter(([, value]) => value === '').map(([key]) => key)
    expect(empty).toEqual([])
  })

  it('switches every text to the chosen language and falls back to Russian', () => {
    expect(applyLanguage('kk')).toBe('kk')
    expect(strings.tabHome).toBe(dictionaries.kk.tabHome)
    expect(document.documentElement.lang).toBe('kk')

    expect(applyLanguage('de')).toBe('ru')
    expect(strings.tabHome).toBe(dictionaries.ru.tabHome)
  })
})
