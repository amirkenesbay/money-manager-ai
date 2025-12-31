# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Telegram bot for personal and shared financial management built with Spring Boot, Kotlin, and the Chat Machinist state machine framework. The bot uses MongoDB for data persistence and supports multi-user group accounting with invite-based sharing.

## Build & Development Commands

### Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Database

```bash
# Start MongoDB via Docker Compose
docker-compose up -d

# Stop MongoDB
docker-compose down

# View MongoDB logs
docker-compose logs -f mongodb
```

MongoDB runs on port 27019 (host) and uses database `moneyManagerBot` with credentials in `application.yml`.

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "ai.moneymanager.service.GroupServiceTest"

# Run tests with output
./gradlew test --info
```

### Code Quality

```bash
# Clean build directory
./gradlew clean

# Check dependencies
./gradlew dependencies

# View dependency tree
./gradlew dependencies --configuration runtimeClasspath
```

## Architecture Overview

### State Machine Pattern (Chat Machinist)

The bot uses a **dialog-based state machine** where user conversations flow through predefined states and transitions:

- **States** (`MoneyManagerState`): Define conversation positions (MENU, GROUP_MANAGEMENT, GROUP_CREATE_ENTER_NAME, etc.)
- **Transitions** (`DialogBuilder`): Define state changes based on events (button clicks, text input, commands)
- **Replies** (`RepliesBuilder`): Map states to message templates with keyboards
- **Context** (`MoneyManagerContext`): Maintains conversation state across transitions

### Key Architectural Layers

1. **Dialog Layer** (`chat/dialog/`): State transitions and conversation flow
2. **Reply Layer** (`chat/reply/`): Message templates and keyboard definitions
3. **Service Layer** (`service/`): Business logic (GroupService, UserInfoService)
4. **Repository Layer** (`repository/`): Data access with Spring Data MongoDB
5. **Domain Layer** (`domain/model/`): Business entities and state definitions

### Configuration Flow

```
Application Start
  ↓
MoneyManagerChatConfig (creates ChatBuilder)
  ↓
MoneyManagerDialog (defines transitions)
  ├─ startMoneyManagerDialogTransition (handles /start, /join)
  ├─ joinGroupDialogTransitions (group invitation flow)
  └─ groupDialogTransitions (group management)
  ↓
MoneyManagerReplyConfig (maps states to replies)
  ├─ menuReply() → MENU state
  ├─ groupManagementReply() → GROUP_MANAGEMENT state
  └─ groupListReply() → GROUP_LIST state
```

## Critical Patterns & Conventions

### Adding New States

1. Add state to `MoneyManagerState` enum
2. Create reply function in appropriate reply file (e.g., `GroupReply.kt`)
3. Register reply in `MoneyManagerReplyConfig`
4. Add transitions in appropriate dialog file (e.g., `GroupDialogs.kt`)

Example:
```kotlin
// 1. Add to State.kt
enum class MoneyManagerState {
    // ...
    MY_NEW_STATE
}

// 2. Create reply in GroupReply.kt
fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.myNewStateReply() {
    reply {
        state = MoneyManagerState.MY_NEW_STATE
        message {
            text = "Message content"
            keyboard {
                buttonRow {
                    button {
                        text = "Button text"
                        type = MoneyManagerButtonType.MY_BUTTON
                    }
                }
            }
        }
    }
}

// 3. Register in MoneyManagerReplyConfig
myNewStateReply()

// 4. Add transition in GroupDialogs.kt
transition {
    name = "Transition to my new state"
    condition {
        from = MoneyManagerState.PREVIOUS_STATE
        button = MoneyManagerButtonType.MY_BUTTON
    }
    then {
        to = MoneyManagerState.MY_NEW_STATE
    }
}
```

### Context Management

Always update `context.userInfo` when state changes affect user data:

```kotlin
action {
    val userId = user.id
    val updatedData = service.updateData(userId)

    // IMPORTANT: Update context to reflect changes
    context.userInfo = context.userInfo?.copy(
        activeGroupId = updatedData.activeGroupId,
        groupIds = updatedData.groupIds
    )
}
```

### Command Handling with Parameters

Commands with deep links or parameters must use `guard` instead of exact `text` matching:

```kotlin
// WRONG - won't match "/start join_TOKEN"
condition {
    eventType = EventType.COMMAND
    text = "/start"
}

// CORRECT - matches "/start" with any parameters
condition {
    eventType = EventType.COMMAND
    guard {
        update.message?.text?.startsWith("/start") == true
    }
}
```

### Text Formatting with trimMargin

For multi-line text with dynamic content, use `trimMargin()` with `|` prefix for consistent alignment:

```kotlin
text = """
    |📋 Title
    |
    |Static text: ${dynamicVariable}
    |
    |$multiLineContent
    |
    |Footer text
""".trimMargin()
```

This prevents indentation issues in Telegram messages.

### Back Button Transitions

Back buttons must specify explicit `from` states - wildcards don't work reliably:

```kotlin
// Add separate transition for each state with back button
transition {
    name = "Back to menu from state X"
    condition {
        from = MoneyManagerState.STATE_X
        button = MoneyManagerButtonType.BACK_TO_MENU
    }
    then {
        to = MoneyManagerState.MENU
    }
}
```

## Data Model Relationships

```
UserInfo
  ├─ telegramUserId: Long (primary identifier)
  ├─ activeGroupId: ObjectId? (current group)
  └─ groupIds: Set<ObjectId> (all groups user belongs to)

MoneyGroup
  ├─ id: ObjectId
  ├─ inviteToken: String (9-char alphanumeric)
  ├─ ownerId: Long (creator's Telegram ID)
  ├─ memberIds: Set<Long> (all member Telegram IDs)
  └─ type: GroupType (PERSONAL or SHARED)
```

**Important**: Always keep `UserInfo.groupIds` and `MoneyGroup.memberIds` in sync when adding/removing users from groups.

## Common Conversation Flows

### Group Creation Flow
```
MENU → (click "Совместный учет")
  → GROUP_MANAGEMENT → (click "Создать группу")
  → GROUP_CREATE_ENTER_NAME → (user types name)
  → [Action: createGroup()]
  → GROUP_INVITE_SHOW → (click "Назад")
  → MENU
```

### Group Join Flow (via invite link)
```
/start join_TOKEN
  → STARTED
  → [Action: fetch group by token]
  → GROUP_JOIN_CONFIRM → (click "Присоединиться")
  → [Action: joinGroup()]
  → MENU
```

## Dependency on Chat Machinist

This project uses two local Chat Machinist dependencies:
- `/media/LocalDisk/work/PET/projects/telegram-bots/chat-machinist` - Core framework
- `/media/LocalDisk/work/PET/projects/telegram-bots/chat-machinist-mongo-persistence` - MongoDB integration

When encountering Chat Machinist-related compilation errors or API questions, scan these projects to understand:
- Transition/Reply builder APIs
- State persistence mechanisms
- Button and event type handling
- Dialog configuration patterns

## MongoDB Configuration

The application uses custom converters for LocalDateTime ↔ Date mapping (see `MongoConfig.kt`). Entity classes use:
- `@Document(collection = "...")` for collection mapping
- `@Id` for MongoDB ObjectId primary keys
- `@CreatedDate` and `@LastModifiedDate` for audit timestamps (requires `@EnableMongoAuditing`)

## Logging & Debugging

State transitions are logged by Chat Machinist with format:
```
chatId=XXX, userId=YYY - Matched Transition "Transition Name" in chat Money Manager Chat
```

Add debug logging in transitions:
```kotlin
action {
    println("🔍 Debug: context.userInfo = ${context.userInfo}")
    // ... business logic
}
```

## Known Issues & Workarounds

1. **"No chat id" errors**: Usually non-fatal warnings from Chat Machinist framework, processing continues normally
2. **"AlertService is not defined"**: Expected warning when alert service isn't configured, doesn't affect functionality
3. **Button transitions not matching**: Ensure button type is defined in `MoneyManagerButtonType` enum and registered in both reply and transition

## Testing the Bot

After code changes:
1. Restart the application: `./gradlew bootRun`
2. Test basic flow: `/start` → verify main menu appears
3. Test group creation: Click "Совместный учет" → "Создать группу" → enter name
4. Test invite flow: Share invite link → open in different Telegram account → verify join process
5. Check MongoDB: Verify data persisted correctly in `userInfos` and `moneyGroups` collections

## Code Style Guidelines

- Use Kotlin idioms: data classes, extension functions, null-safety (`?.`, `?:`)
- Prefer immutability: use `val` over `var`, immutable collections
- Service methods should be small and focused (Single Responsibility)
- Avoid business logic in Dialog/Reply builders - delegate to Services
- Use meaningful names: `createGroupTransition()` not `transition1()`
- Enum naming: `UPPER_SNAKE_CASE` for states/button types, `PascalCase` for classes