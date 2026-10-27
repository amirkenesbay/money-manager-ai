import type { CategoryType } from './api/types'
import type { Period } from './operations/periods'

const PERIOD_LABELS: Record<Period, string> = {
  last30Days: 'Последние 30 дней',
  thisMonth: 'Этот месяц',
  lastMonth: 'Прошлый месяц',
  last3Months: 'Последние 3 месяца',
  allTime: 'Всё время',
}

const TYPE_FILTER_LABELS: Record<CategoryType | 'ALL', string> = {
  ALL: 'Все',
  EXPENSE: 'Расходы',
  INCOME: 'Доходы',
}

const MEMBER_FORMS: Record<Intl.LDMLPluralRule, string> = {
  zero: 'участников',
  one: 'участник',
  two: 'участника',
  few: 'участника',
  many: 'участников',
  other: 'участника',
}

const russianPlural = new Intl.PluralRules('ru')

export const strings = {
  outsideTelegramIcon: '🔒',
  outsideTelegramTitle: 'Открой через Telegram',
  outsideTelegramHint: 'Приложение работает только внутри Telegram — так мы знаем, что это именно ты, без паролей',
  loading: 'Загружаем данные…',
  loadFailedIcon: '⚠️',
  loadFailedTitle: 'Что-то пошло не так',
  loadFailedFallback: 'Сервер не ответил. Попробуй ещё раз чуть позже — твои данные в порядке',
  retry: 'Попробовать снова',
  greetingLabel: 'привет,',
  defaultName: 'друг',
  noGroupIcon: '👥',
  noGroupTitle: 'Пока нет группы',
  noGroupHint: 'Открой бота и выбери язык — личная группа создастся автоматически, и здесь появится твой бюджет',
  groupLabel: 'Группа',
  groupSwitchFailed: 'Не удалось переключить группу. Попробуй ещё раз',
  balanceTitle: 'Баланс',
  reportLink: 'Отчёт',
  monthExpense: (amount: string) => `${amount} расх.`,
  monthIncome: (amount: string) => `${amount} дох.`,
  monthNoExpense: 'расходов пока нет',
  monthOverspent: (amount: string) => `Расходы за месяц превысили доходы на ${amount}`,
  recentOperationsTitle: 'Последние',
  recentOperationsEmpty: 'Здесь появятся твои операции. Нажми на микрофон внизу и скажи первую трату — я сам разберу сумму и категорию',
  allOperations: 'Все →',
  voiceTitle: 'Нажми и скажи',
  voiceTitleFirst: 'Скажи первую трату',
  voiceExample: '«Потратил 5 000 на продукты»',
  recordVoice: 'Записать голосом',
  manualEntry: 'Вручную',
  manualEntryLabel: 'Ввести вручную',
  comingSoon: 'Скоро будет. Пока записывай операции в боте',
  back: '← Назад',
  operationsTitle: 'Операции',
  addOperation: 'Добавить операцию',
  sharedGroupSubtitle: (name: string, members: number) => `группа «${name}» · ${members} ${MEMBER_FORMS[russianPlural.select(members)]}`,
  today: 'Сегодня',
  yesterday: 'Вчера',
  typeFilterLabel: 'Тип операций',
  typeFilter: (type: CategoryType | null) => TYPE_FILTER_LABELS[type ?? 'ALL'],
  periodFilterLabel: 'Период',
  period: (period: Period) => PERIOD_LABELS[period],
  categoryFilterLabel: 'Категория',
  allCategories: 'Все категории',
  operationsLoading: 'Загрузка операций',
  operationsEmptyIcon: '🔍',
  operationsEmptyTitle: 'Ничего не найдено',
  operationsEmptyHint: 'Попробуй выбрать другой период или категорию',
  resetFilters: 'Сбросить фильтры',
  operationsEnd: 'Это все операции за период',
  loadMore: 'Показать ещё',
  loadMoreFailed: 'Не удалось загрузить продолжение. Попробуй ещё раз',
  premiumHistoryTitle: (days: number | null) => `Старше ${days ?? 0} дней — в Premium`,
  premiumHistoryHint: 'Вся история и отчёты за любой период',
}
