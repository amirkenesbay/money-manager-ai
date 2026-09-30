# 02 — Архитектура

## Общая схема

```
Telegram-клиент
      │
      ├── Бот (chat-machinist) ──┐
      │                          │
      └── Mini App (React) ──────┤
              │ HTTPS + initData │
              ▼                  ▼
        REST-контроллеры    Transitions/Replies
              │                  │
              └────────┬─────────┘
                       ▼
              Сервисный слой (общий)
                       ▼
                Repositories → MongoDB
```

Ключевой принцип: **webapp и бот — два входа в одну систему**. Оба вызывают один и тот же
сервисный слой. Бизнес-правила (лимиты тарифа, проверки владельца, валидация) живут в сервисах,
а не в контроллерах и не в UI.

## Что уже готово

Ветка `feature/TELEGRAM-WEBAPP`, коммит `5b6642d`:

- `web/security/TelegramInitDataValidator` — валидация подписи initData (HMAC-SHA256 по алгоритму Telegram), проверка TTL 24 ч.
- `web/security/TelegramPrincipal` — модель авторизованного пользователя.
- `TelegramInitDataValidatorTest` — тесты валидатора.
- `spring-boot-starter-web` в зависимостях, `server.port` в конфиге.

Чего нет: фильтра аутентификации, контроллеров, DTO, фронтенда, CORS, хостинга.

## Проблема, требующая решения: форматтеры возвращают Telegram-HTML

Обнаружено при разведке. `FinanceHistoryService.generateReport()` и все
`FinanceReportService.generate*Report()` возвращают **готовую строку с Telegram-HTML**
(`<b>`, `<pre>`, ASCII progress-бары). Для webapp это непригодно — нужны структурированные данные.

Хорошая новость: внутри `FinanceReportService` приватные методы `buildComparisonReport()`,
`buildAnalyticsReport()`, `buildMembersReport()`, `buildCategoryReport()` уже возвращают
чистые data-классы из `domain/model/report/ReportData.kt`.

**Решение:** поднять видимость `build*` методов до публичных и отдавать их результат в API.
Форматирование в Telegram-HTML остаётся отдельным слоем только для бота. Дублирования расчётов нет.

Для истории операций (`FinanceHistoryService`) аналога нет — там расчёт и форматирование
слиты в одном методе. Понадобится выделить data-часть (суммы по категориям, итоги) отдельно.

## Структура проекта

Gradle-модули, один исполняемый jar (решение D-009, задачи W-023 … W-025):

```
money-manager-ai/
├── core/   — ядро: domain, repository, service, config; messages*, prompts/
│           └── src/testFixtures — общие тестовые фикстуры (ai.moneymanager.testing)
├── bot/    — Telegram-бот: chat/** (диалоги, ответы, рендер), chat.telegram — Telegram-сервисы
├── web/    — REST API Mini App: web/** (security, access, error, json, me, finance)
├── app/    — MoneyManagerAIApplication, application.yml, logback.xml → один jar
└── webapp/ — фронтенд (React + Vite)
    ├── src/api/        — HTTP-клиент, типы ответов
    ├── src/telegram/   — инициализация Telegram SDK
    ├── dev/            — мок API и генератор подписанных ссылок для разработки
    └── dist/           — сборка
```

Зависимости: `:bot` → `:core`, `:web` → `:core`, `:app` → все. `:core` и `:web` не видят бота
и Telegram-библиотеки — нарушение границы не компилируется.

## Аутентификация

1. Mini App при старте получает `window.Telegram.WebApp.initData` — подписанную строку от Telegram.
2. Фронтенд шлёт её в каждом запросе в заголовке `Authorization: tma <initData>`.
3. Сервлет-фильтр валидирует подпись через `TelegramInitDataValidator`, кладёт `TelegramPrincipal` в запрос.
4. Контроллеры получают принципала и по `userId` резолвят `UserInfo` через существующий `UserInfoService`.
5. Невалидная или просроченная initData → `401`.

Сессий и токенов нет — initData самодостаточна и проверяется на каждом запросе.

## Авторизация доступа к данным

Все данные принадлежат группам. На каждом запросе, работающем с группой:

1. Резолвим `UserInfo` по `telegramUserId` из принципала.
2. Проверяем, что запрошенный `groupId` входит в `userInfo.groupIds`; иначе `403`.
3. Операции владельца (удаление группы, смена валюты, переименование) дополнительно проверяют `ownerId`.

Проверка выполняется на бэкенде всегда, независимо от того, что показывает UI.

## Формат API

- Префикс: `/api/v1`.
- JSON, UTF-8.
- Идентификаторы (`ObjectId`) отдаются строками.
- Денежные суммы — строки, чтобы не терять точность `BigDecimal` в JavaScript.
- Даты — ISO-8601 (`2026-07-23`).
- Ошибки — единый формат с машинным кодом и человекочитаемым сообщением на языке пользователя.

Детали — в `03-API.md`.

## Фронтенд

- **React + Vite + TypeScript.**
- **`@telegram-apps/sdk-react`** — доступ к initData, теме, кнопкам Telegram.
- **Графики:** библиотека для диаграмм (выбор при реализации; критерий — лёгкость и поддержка тёмной темы).
- **Стили:** CSS-переменные Telegram (`--tg-theme-bg-color` и др.) — тема подхватывается автоматически.
- **Состояние:** серверные данные через кэширующий слой (react-query или аналог), локальное — через хуки React.

## Хостинг

Та же VM, где крутится бот (`93.170.72.82`). nginx как reverse-proxy:

```
Домен (HTTPS, Let's Encrypt)
  ├── /            → статика фронтенда (webapp/dist)
  └── /api/*       → Spring Boot (localhost:8080)
```

Так фронт и API на одном origin — CORS не нужен, initData не утекает на сторонние домены.

Требуется: домен, DNS на VM, nginx с TLS, автообновление сертификата, деплой статики в CI/CD.

## Влияние на существующий код

Минимальное. Бот не трогаем. Изменения в общем коде:

1. Публикация `build*`-методов в `FinanceReportService` (private → public).
2. Выделение data-части в `FinanceHistoryService`.
3. Возможные вспомогательные методы в сервисах, если API потребует выборок, которых сейчас нет.

Всё остальное — новый код в `web/` и `webapp/`.
