export const LANGUAGES = ['ru', 'en', 'kk'] as const

export type Language = (typeof LANGUAGES)[number]

export const DEFAULT_LANGUAGE: Language = 'ru'

export const LANGUAGE_NAMES: Record<Language, string> = {
  ru: 'Русский',
  en: 'English',
  kk: 'Қазақша',
}

export const isLanguage = (value: string): value is Language => (LANGUAGES as readonly string[]).includes(value)
