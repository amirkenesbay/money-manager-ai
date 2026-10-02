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

export const strings = {
  outsideTelegramTitle: '📱 Откройте приложение из Telegram',
  outsideTelegramHint: 'Money Manager работает внутри бота — откройте его оттуда.',
  loading: 'Загружаем данные…',
  loadFailedTitle: '⚠️ Не удалось загрузить данные',
  loadFailedFallback: 'Проверьте подключение к интернету.',
  retry: 'Повторить',
  greeting: (name: string) => `👋 Привет, ${name}!`,
  defaultName: 'друг',
  noGroupHint: 'Сначала запустите бота — он создаст вашу первую группу. Потом вернитесь сюда.',
  dashboardTitle: '💰 Мои финансы',
  groupLabel: 'Группа',
  groupSwitchFailed: 'Не удалось переключить группу. Попробуйте ещё раз.',
  balanceTitle: 'Баланс',
  balanceInitial: 'Начальный',
  balanceIncome: 'Доходы',
  balanceExpense: 'Расходы',
  monthSummaryTitle: (month: string) => `📅 ${month}`,
  monthNet: 'Итог',
  monthEmpty: 'В этом месяце операций пока нет.',
  topExpensesTitle: 'Больше всего трат',
  recentOperationsTitle: '🕒 Последние операции',
  recentOperationsEmpty: 'Операций пока нет — запишите первую трату в боте.',
  allOperations: 'Все операции →',
  back: '← Назад',
  operationsTitle: '📋 Операции',
  filtersTitle: 'Фильтры',
  typeFilterLabel: 'Тип операций',
  typeFilter: (type: CategoryType | null) => TYPE_FILTER_LABELS[type ?? 'ALL'],
  periodFilterLabel: 'Период',
  period: (period: Period) => PERIOD_LABELS[period],
  categoryFilterLabel: 'Категория',
  allCategories: 'Все категории',
  operationsEmptyTitle: 'Ничего не нашлось',
  operationsEmptyHint: 'Попробуйте выбрать другой период или категорию.',
  loadMore: 'Показать ещё',
  loadMoreFailed: 'Не удалось загрузить продолжение. Попробуйте ещё раз.',
  premiumTitle: '💎 Нужен Premium',
  premiumHistoryLimit: (days: number | null) => `На Free доступна история только за последние ${days ?? 0} дней. С Premium — вся история.`,
  premiumSubscribe: 'Оформить Premium',
}
