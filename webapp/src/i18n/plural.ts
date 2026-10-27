export type PluralForms = Record<Intl.LDMLPluralRule, string>

export const pluralOf = (rules: Intl.PluralRules, forms: PluralForms) => (count: number): string => forms[rules.select(count)]
