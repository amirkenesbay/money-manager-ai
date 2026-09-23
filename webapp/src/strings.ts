import type { SubscriptionTier } from './api/types'

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
  tier: (tier: SubscriptionTier) => `Тариф: ${tier === 'PAID' ? 'Premium ✨' : 'Free'}`,
}
