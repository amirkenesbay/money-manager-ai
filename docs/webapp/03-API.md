# 03 — Контракт API

Базовый префикс: `/api/v1`. Все запросы требуют заголовок `Authorization: tma <initData>`.

## Соглашения

| Тип | Формат | Пример |
|---|---|---|
| Идентификатор | строка (ObjectId в hex) | `"6650a1b2c3d4e5f6a7b8c9d0"` |
| Сумма | строка (точность BigDecimal) | `"12500.00"` |
| Дата | ISO-8601 | `"2026-07-23"` |
| Дата-время | ISO-8601 UTC | `"2026-07-23T14:30:00Z"` |
| Валюта | код | `"KZT"` |
| Тип операции | enum | `"EXPENSE"` \| `"INCOME"` |

## Формат ошибки

```json
{
  "code": "GROUP_LIMIT_REACHED",
  "message": "На Free можно создать до 3 совместных групп",
  "details": { "limit": 3 }
}
```

| HTTP | Когда |
|---|---|
| 400 | Невалидные входные данные |
| 401 | initData отсутствует, невалидна или просрочена |
| 403 | Пользователь не состоит в группе / не владелец |
| 404 | Объект не найден |
| 409 | Конфликт (дубликат имени категории/группы) |
| 422 | Упор в лимит тарифа |

Коды ошибок соответствуют существующим sealed-результатам:
`CATEGORY_DUPLICATE`, `CATEGORY_LIMIT_REACHED`, `GROUP_DUPLICATE`, `GROUP_LIMIT_REACHED`,
`NOTIFICATION_LIMIT_REACHED`, `PREMIUM_REQUIRED`.

---

## Профиль и подписка

### `GET /me`
Профиль текущего пользователя, его группы и статус подписки.

```json
{
  "userId": 319593809,
  "firstName": "Амир",
  "username": "amir",
  "language": "ru",
  "timezone": "Asia/Almaty",
  "activeGroupId": "6650...",
  "subscription": {
    "tier": "FREE",
    "expiresAt": null,
    "limits": {
      "aiRequestsPerDay": 10,
      "categoriesPerType": 10,
      "ownedSharedGroups": 3,
      "activeNotifications": 3,
      "historyDaysBack": 30
    }
  }
}
```

### `PATCH /me`
Смена языка. Тело: `{ "language": "en" }`.

### `GET /me/subscription`
Детали тарифа для экрана Premium: текущий тариф, срок, ссылка на оплату.

---

## Группы

### `GET /groups`
Список групп пользователя.

```json
[
  {
    "id": "6650...",
    "name": "Семья",
    "type": "SHARED",
    "currency": "KZT",
    "isOwner": true,
    "isActive": true,
    "memberCount": 3,
    "initialBalance": "50000.00"
  }
]
```

### `POST /groups`
Создание совместной группы. Тело: `{ "name": "Семья", "currency": "KZT" }`.
Ошибки: `409 GROUP_DUPLICATE`, `422 GROUP_LIMIT_REACHED`.

### `PATCH /groups/{groupId}`
Переименование и/или смена валюты (только владелец). Тело: `{ "name": "...", "currency": "USD" }`.

### `DELETE /groups/{groupId}`
Удаление группы со всеми категориями (только владелец).

### `POST /groups/{groupId}/activate`
Сделать группу активной для пользователя.

### `GET /groups/{groupId}/members`
Участники группы: имя, признак владельца.

### `GET /groups/{groupId}/invite`
Invite-ссылка: `{ "inviteToken": "ABC123XYZ", "url": "https://t.me/..." }`.

### `PUT /groups/{groupId}/initial-balance`
Начальный баланс. Тело: `{ "amount": "50000.00" }`.

---

## Баланс и операции

### `GET /groups/{groupId}/balance`
```json
{
  "currency": "KZT",
  "initial": "50000.00",
  "income": "300000.00",
  "expense": "180000.00",
  "total": "170000.00"
}
```

### `GET /groups/{groupId}/operations`
Параметры: `from`, `to` (даты), `type`, `categoryId`, `page`, `size`.

```json
{
  "items": [
    {
      "id": "6650...",
      "type": "EXPENSE",
      "amount": "3500.00",
      "categoryId": "6651...",
      "categoryName": "Продукты",
      "categoryIcon": "🛒",
      "operationDate": "2026-07-22",
      "description": "Магнум",
      "creatorId": 319593809,
      "creatorName": "Амир"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 143
}
```
Ошибка `422 PREMIUM_REQUIRED`, если `from` глубже лимита Free.

### `POST /groups/{groupId}/operations`
Создание. Тело: `type`, `amount`, `categoryId`, `operationDate`, `description`.

### `PATCH /groups/{groupId}/operations/{operationId}`
Частичное обновление любого поля.

### `DELETE /groups/{groupId}/operations/{operationId}`
Удаление.

---

## Отчёты

Все отчёты отдают **данные**, не размеченный текст. Источник — публичные `build*`-методы `FinanceReportService`
(модели описаны в `domain/model/report/ReportData.kt`).

### `GET /groups/{groupId}/reports/analytics?month=2026-07`
Аналитика за месяц: суммы, количество операций, топ категорий, максимальная трата, самый дорогой день.

### `GET /groups/{groupId}/reports/comparison?month=2026-07`
Сравнение месяца с предыдущим, включая разбивку по категориям.

### `GET /groups/{groupId}/reports/members?month=2026-07`
Разбивка по участникам группы.

### `GET /groups/{groupId}/reports/category?categoryId=...&months=6`
Динамика по категории за N месяцев.

Для всех: `422 PREMIUM_REQUIRED`, если запрошенный период глубже лимита Free.

---

## Категории

### `GET /groups/{groupId}/categories`
Параметр `type` — опциональный фильтр.

```json
[
  { "id": "6651...", "name": "Продукты", "icon": "🛒", "type": "EXPENSE" }
]
```

### `POST /groups/{groupId}/categories`
Тело: `{ "name": "Кафе", "icon": "☕", "type": "EXPENSE" }`.
Ошибки: `409 CATEGORY_DUPLICATE`, `422 CATEGORY_LIMIT_REACHED`.

### `PATCH /groups/{groupId}/categories/{categoryId}`
Переименование и/или смена иконки.

### `DELETE /groups/{groupId}/categories/{categoryId}`
Удаление.

---

## Уведомления

Привязаны к пользователю, не к группе.

### `GET /notifications`
```json
[
  {
    "id": "6652...",
    "name": "Записать траты",
    "icon": "💸",
    "isActive": true,
    "frequencyType": "DAILY",
    "hour": 21,
    "minute": 0,
    "nextFireTime": "2026-07-23T21:00:00Z"
  }
]
```

### `POST /notifications`
Тело: `name`, `icon`, `frequencyType`, `customInterval`, `hour`, `minute`, `dayOfWeek`, `dayOfMonth`, `monthOfYear`.
Ошибка: `422 NOTIFICATION_LIMIT_REACHED`.

### `PATCH /notifications/{id}`
Изменение полей и/или переключение `isActive`.

### `DELETE /notifications/{id}`
Удаление.
