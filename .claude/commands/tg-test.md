Test the Money Manager AI bot via Telegram MCP (telegram-mcp).

## Bot Info

- Bot username: `@moneyManagerAIbot`
- Bot chat ID: `8554230883`

## Available MCP Tools

- `mcp__telegram-mcp__send_message` — send a text message to the bot
- `mcp__telegram-mcp__list_messages` — read recent messages in chat
- `mcp__telegram-mcp__list_inline_buttons` — inspect inline buttons on a message
- `mcp__telegram-mcp__press_inline_button` — press an inline button by text or index

## Instructions

### Interpreting $ARGUMENTS

If `$ARGUMENTS` is provided, treat it as a test scenario to execute. Examples:
- "create category Кофе expense" — go through the full category creation flow
- "create group Семья" — go through group creation flow
- "press Категории" — navigate to categories from current state
- "send /start" — send /start command
- "status" — show current bot state (last messages + buttons)

If no arguments, show current bot state.

### Execution Rules

1. **Always use chat_id `8554230883`** for all MCP calls.

2. **After every action** (send_message or press_inline_button), call `list_messages` with limit=2 to see the bot's response, then `list_inline_buttons` on the latest bot message to see available buttons.

3. **Telegram edits are invisible**: The bot edits messages in-place via `editMessageText`. The MCP `list_messages` may show stale text for edited messages. Use `list_inline_buttons` on the latest bot message — button changes ARE reflected and indicate the true current state.

4. **Report each step** concisely:
   ```
   Step: <what you did>
   Bot: <bot response text or "edited message">
   Buttons: <list of available buttons>
   ```

5. **For multi-step flows**, execute all steps automatically without asking for confirmation between steps.

6. **If the bot doesn't respond** within the expected flow, check the last 5 messages and buttons to diagnose the state.

### Common Flows Reference

**Category creation**: /start -> "Категории" -> "Создать категорию" -> select type -> type name
**Group creation**: /start -> "Совместный учет" -> "Создать группу" -> type name
**View categories**: /start -> "Категории" -> "Мои категории"

$ARGUMENTS