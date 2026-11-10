interface CurrencyInfo {
  symbol: string
  unit: number
}

export const CURRENCY_CATALOG = {
  KZT: { symbol: '₸', unit: 500 },
  USD: { symbol: '$', unit: 1 },
  RUB: { symbol: '₽', unit: 100 },
  EUR: { symbol: '€', unit: 1 },
  KGS: { symbol: 'с', unit: 100 },
  BYN: { symbol: 'Br', unit: 5 },
  UZS: { symbol: 'сўм', unit: 10_000 },
  UAH: { symbol: '₴', unit: 50 },
  GEL: { symbol: '₾', unit: 5 },
  AMD: { symbol: '֏', unit: 500 },
  AZN: { symbol: '₼', unit: 2 },
  TJS: { symbol: 'смн', unit: 10 },
  TMT: { symbol: 'm', unit: 5 },
  MDL: { symbol: 'L', unit: 20 },
  TRY: { symbol: '₺', unit: 50 },
  AED: { symbol: 'Dh', unit: 5 },
  GBP: { symbol: '£', unit: 1 },
  CNY: { symbol: 'CN¥', unit: 10 },
  JPY: { symbol: '¥', unit: 100 },
  KRW: { symbol: '₩', unit: 1_000 },
  THB: { symbol: '฿', unit: 50 },
  INR: { symbol: '₹', unit: 100 },
  IDR: { symbol: 'Rp', unit: 10_000 },
  VND: { symbol: '₫', unit: 20_000 },
  MNT: { symbol: '₮', unit: 5_000 },
  PLN: { symbol: 'zł', unit: 5 },
  CZK: { symbol: 'Kč', unit: 20 },
  CHF: { symbol: 'Fr', unit: 1 },
  CAD: { symbol: 'C$', unit: 1 },
  AUD: { symbol: 'A$', unit: 2 },
  ILS: { symbol: '₪', unit: 5 },
  EGP: { symbol: 'E£', unit: 50 },
  SAR: { symbol: 'SR', unit: 5 },
  QAR: { symbol: 'QR', unit: 5 },
} as const satisfies Record<string, CurrencyInfo>

export type Currency = keyof typeof CURRENCY_CATALOG

export const CURRENCIES = Object.keys(CURRENCY_CATALOG) as Currency[]

const FIRST_LETTER = /^\p{L}/u

export function currencyDisplayName(currency: Currency, language: string): string {
  try {
    const name = new Intl.DisplayNames([language], { type: 'currency' }).of(currency)
    return name && name !== currency ? name.replace(FIRST_LETTER, (letter) => letter.toLocaleUpperCase(language)) : currency
  } catch {
    return currency
  }
}
