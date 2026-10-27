import { en } from './i18n/en'
import { kk } from './i18n/kk'
import { DEFAULT_LANGUAGE, isLanguage, type Language } from './i18n/languages'
import { ru, type Strings } from './i18n/ru'

const DICTIONARIES: Record<Language, Strings> = { ru, en, kk }

export const strings: Strings = { ...ru }

export function applyLanguage(language: string): Language {
  const supported = isLanguage(language) ? language : DEFAULT_LANGUAGE
  Object.assign(strings, DICTIONARIES[supported])
  document.documentElement.lang = supported
  return supported
}

export const dictionaries = DICTIONARIES
