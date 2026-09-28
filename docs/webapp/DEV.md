# Разработка Mini App локально

Три способа — от быстрого к полному. Выбирай по задаче.

| Способ | Что нужно | Когда |
|---|---|---|
| 1. UI на моке | Node 22 | Вёрстка, состояния экранов, тексты |
| 2. Браузер + локальный бэкенд | Node 22, JDK 21, Docker, **тестовый бот** | API, авторизация, реальные данные из Mongo |
| 3. Настоящий Telegram | всё из п. 2 + туннель | Тема, кнопки Telegram, поведение в клиенте |

## ⚠️ Прежде всего: отдельный тестовый бот

Локальный бэкенд поднимает не только API, но и **бота** (long polling). Если запустить его с токеном
продового бота, два процесса начнут забирать апдейты друг у друга — прод будет терять сообщения.

1. В [@BotFather](https://t.me/BotFather): `/newbot` → например `money_manager_dev_bot`.
2. Сохрани токен в `.env.dev` в корне репозитория (файл в `.gitignore`, в git не попадёт):

```bash
BOT_NAME=money_manager_dev_bot
BOT_TOKEN=<токен ТЕСТОВОГО бота>
GEMINI_API_KEY=<ключ>
MONGO_HOST=localhost
MONGO_USERNAME=<как в docker-entrypoint-initdb.d/mongo-init.js>
MONGO_PASSWORD=<как в docker-entrypoint-initdb.d/mongo-init.js>
```

Продовый `.env` для локальной разработки не используй.

## Подготовка фронтенда

```bash
cd webapp
nvm use          # Node 22 из webapp/.nvmrc
npm ci
```

Проверки: `npm test`, `npm run lint`, `npm run build`.

## 1. UI на моке

```bash
cd webapp
npm run dev:mock
```

Бэкенд не нужен: `/api/v1/me` отвечает мок из `webapp/dev/mockApi.ts`.
Приложение показывает «Откройте из Telegram», пока в адресе нет launch-параметров — сгенерируй ссылку
с любым токеном (мок подпись не проверяет):

```bash
BOT_TOKEN=123456:ANY npm run -s dev:launch-url
```

Сценарий выбирается параметром `?mock=` **перед** `#`:

| `?mock=` | Что увидишь |
|---|---|
| `ok` (по умолчанию) | Приветствие, тариф Free |
| `premium` | Приветствие, Premium |
| `nogroup` | «Сначала запустите бота» |
| `error` | Ошибка сервера и «Повторить» |
| `flaky` | Ошибка, затем «Повторить» срабатывает |
| `slow` | Загрузка 4 секунды |

Пример: `http://localhost:5173/?mock=nogroup#tgWebAppData=…`

Тема Telegram в обычном браузере не применяется — SDK не с кем связаться; работают запасные цвета
(светлые/тёмные по системе). Удобнее смотреть в DevTools → режим устройства, ширина ~390px.

## 2. Браузер + локальный бэкенд

```bash
docker compose up -d                                   # Mongo на :27019

set -a; source .env.dev; set +a
./gradlew bootRun                                      # JDK 21, API на :8080

cd webapp && npm run dev                               # Vite на :5173, /api → :8080
```

Ссылка с initData, **подписанной токеном тестового бота** — бэкенд проверит подпись по-настоящему:

```bash
cd webapp
set -a; source ../.env.dev; set +a
npm run -s dev:launch-url
```

Необязательные переменные: `DEV_USER_ID` (по умолчанию `1000001`), `DEV_FIRST_NAME`,
`DEV_APP_URL` (по умолчанию `http://localhost:5173/`). Другой адрес бэкенда — `API_PROXY_TARGET`.

Первый вход создаёт пользователя в локальной базе без групп — приложение попросит запустить бота
(решение D-007). Чтобы появилась группа, напиши тестовому боту `/start`, выбери язык и валюту
с того же Telegram-аккаунта и укажи его id в `DEV_USER_ID`.

## 3. Настоящий Telegram через туннель

Telegram открывает Mini App только по HTTPS. Туннель даёт временный HTTPS-адрес на локальный Vite.

```bash
brew install cloudflared                               # один раз
cloudflared tunnel --url http://localhost:5173         # печатает https://<случайно>.trycloudflare.com
```

Бэкенд и `npm run dev` должны быть запущены, как в п. 2.

В @BotFather для **тестового** бота: `/mybots` → бот → **Bot Settings** → **Menu Button** →
вставь адрес туннеля. В чате с ботом появится кнопка — она открывает Mini App.

- Адрес quick-туннеля меняется при каждом запуске `cloudflared` — кнопку меню придётся обновить.
- Vite пускает хосты `*.trycloudflare.com`, `*.ngrok-free.app`, `*.ngrok-free.dev` (`server.allowedHosts`).
- ngrok тоже работает (`ngrok http 5173`), но бесплатный тариф показывает промежуточную страницу
  при первом открытии — в Telegram это мешает.
- Правки в `webapp/src` подхватываются без перезапуска.

## Частые проблемы

| Симптом | Причина |
|---|---|
| «Откройте приложение из Telegram» | В адресе нет launch-параметров (`#tgWebAppData=…`) |
| «Не удалось подтвердить вход через Telegram» | initData подписана другим токеном или старше 24 часов — сгенерируй ссылку заново |
| «Проверьте подключение к интернету» | Бэкенд не запущен или `API_PROXY_TARGET` указывает не туда |
| `Blocked request. This host is not allowed` | Туннель не из списка `allowedHosts` в `vite.config.ts` |
| Прод-бот перестал отвечать | Локальный бэкенд запущен с продовым токеном — останови и возьми `.env.dev` |
