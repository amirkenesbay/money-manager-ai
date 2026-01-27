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

### Environment Variables

Required configuration in `application.yml`:
- `GEMINI_API_KEY` - Google Gemini API key for NLP (defaults to hardcoded value if not set)
- `MONGO_HOST` - MongoDB host (defaults to `localhost`)

Bot token is hardcoded in `application.yml` (not recommended for production).

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

### Context Fields

The `MoneyManagerContext` class stores temporary conversation state:

**User State:**
- `userInfo: UserInfo?` - Current user's persisted data (activeGroupId, groupIds)

**Group Management:**
- `pendingInviteToken: String?` - Token from /start join_TOKEN command
- `pendingGroup: MoneyGroup?` - Group to join (fetched by invite token)
- `pendingGroupOwnerInfo: UserInfo?` - Owner info for confirmation screen
- `currentGroup: MoneyGroup?` - Currently selected/created group
- `groupNameInput: String?` - User's input for group name
- `userGroups: List<MoneyGroup>` - User's groups for selection lists
- `isQuickGroupCreation: Boolean` - Flag for quick group creation flow

**Category Management:**
- `categoryNameInput: String?` - User's input for category name
- `categoryIconInput: String?` - User's input for category icon (emoji)
- `categoryTypeInput: CategoryType?` - EXPENSE or INCOME
- `currentCategory: Category?` - Selected category for editing/deletion
- `categories: List<Category>` - Categories in current group
- `categoriesCountToDelete: Int` - Number of categories to be deleted

**NLP Processing:**
- `nlpGroupName: String?` - Group name parsed from NLP
- `nlpResponse: String?` - Generic response message for out-of-context queries

**IMPORTANT**: Context is mutable and shared across transitions. Always populate context in `action {}` blocks before transitioning to states that depend on it.

### Key Architectural Layers

1. **Dialog Layer** (`chat/dialog/`): State transitions and conversation flow
2. **Reply Layer** (`chat/reply/`): Message templates and keyboard definitions
3. **Service Layer** (`service/`): Business logic (GroupService, UserInfoService, CategoryService, CommandParserService)
4. **Repository Layer** (`repository/`): Data access with Spring Data MongoDB
5. **Domain Layer** (`domain/model/`): Business entities, state definitions, and NLP models

### Configuration Flow

```
Application Start
  ↓
MoneyManagerChatConfig (creates ChatBuilder)
  ↓
MoneyManagerDialog (defines transitions)
  ├─ startMoneyManagerDialogTransition (handles /start, /join)
  ├─ joinGroupDialogTransitions (group invitation flow)
  ├─ groupDialogTransitions (group management)
  ├─ categoryDialogTransitions (category management)
  └─ nlpDialogTransitions (NLP command processing)
  ↓
MoneyManagerReplyConfig (maps states to replies)
  ├─ menuReply() → MENU state
  ├─ groupManagementReply() → GROUP_MANAGEMENT state
  ├─ categoryManagementReply() → CATEGORY_MANAGEMENT state
  └─ nlpResponseReply() → NLP_RESPONSE state
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

### NLP Processing with Triggered Transitions

NLP transitions use a special pattern to process user input and immediately transition to confirmation/response:

```kotlin
transition {
    name = "Process text via NLP"
    condition {
        from = MoneyManagerState.MENU
        eventType = EventType.TEXT
    }
    action {
        // Parse command and populate context
        processNlpMessage(userMessage, context, commandParserService)
    }
    then {
        to = MoneyManagerState.MENU
        noReply = true  // Don't send reply yet
        trigger { sameDialog = true }  // Trigger another transition immediately
    }
}

// Follow-up triggered transition checks context and routes to appropriate state
transition {
    name = "Show NLP confirmation"
    condition {
        from = MoneyManagerState.MENU
        eventType = EventType.TRIGGERED
        guard { context.nlpGroupName != null }  // Check if NLP populated context
    }
    then {
        to = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
    }
}
```

This pattern allows NLP processing to populate context, then immediately evaluate which state to transition to based on the parsed command.

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
  ├─ name: String
  ├─ inviteToken: String (9-char alphanumeric, unique)
  ├─ ownerId: Long (creator's Telegram ID)
  ├─ memberIds: Set<Long> (all member Telegram IDs)
  └─ type: GroupType (PERSONAL or SHARED)

Category
  ├─ id: ObjectId
  ├─ name: String
  ├─ icon: String? (emoji)
  ├─ type: CategoryType (EXPENSE or INCOME)
  └─ groupId: ObjectId (compound unique with name)
```

**MongoDB Collections**: `money_manager_user`, `money_group`, `category`

**Important**: Always keep `UserInfo.groupIds` and `MoneyGroup.memberIds` in sync when adding/removing users from groups.

**Default Categories**: When a group is created, default categories are auto-created:
- Expenses: Продукты, Транспорт, Развлечения, Здоровье, Одежда, Образование, Другое
- Income: Зарплата, Подарки, Другое

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

### NLP Text Command Flow
```
User sends text: "Создай группу Семья"
  → [CommandParserService.parseCommand() via Gemini]
  → CreateGroup command detected
  → NLP_CONFIRM_CREATE_GROUP → (click "Создать")
  → [Action: createGroup()]
  → GROUP_INVITE_SHOW
```

### NLP Voice Command Flow
```
User sends voice message
  → [TelegramFileService downloads audio bytes]
  → [CommandParserService.parseVoiceCommand() via Gemini]
  → Gemini transcribes and calls function
  → Same flow as text NLP processing
```

## Gemini NLP Integration

The bot uses Google Gemini 2.5 Flash for natural language command parsing via function calling. Supports both text and voice messages.

### Key Components

- **CommandParserService** (`service/CommandParserService.kt`): Parses user text/voice into typed `BotCommand` objects using Gemini function calling
- **TelegramFileService** (`service/TelegramFileService.kt`): Downloads voice messages from Telegram API
- **NlpDialogs** (`chat/dialog/NlpDialogs.kt`): Handles NLP-triggered state transitions
- **GeminiArgsMapper** (`mapper/GeminiArgsMapper.kt`): Maps Gemini function call arguments to Kotlin data classes

### Configuration

Set Gemini API key in `application.yml` or environment variable:
```yaml
gemini:
  api-key: ${GEMINI_API_KEY:your-key-here}
  model: gemini-2.5-flash
```

### Available NLP Functions

Defined in `BotFunctions.kt`:
- `createGroup(name)` - Create a new group
- `addExpense(amount, category, description?)` - Record an expense (planned)
- `addIncome(amount, category, description?)` - Record income (planned)
- `outOfContext(userMessage)` - Fallback for non-financial queries

### Voice Message Processing

Voice messages are automatically transcribed by Gemini and processed like text:

1. User sends voice message in MENU or NLP_RESPONSE state
2. `TelegramFileService.downloadVoice()` fetches audio bytes from Telegram
3. `CommandParserService.parseVoiceCommand()` sends audio to Gemini with prompt
4. Gemini transcribes speech and calls appropriate function
5. Same flow as text NLP processing continues

**Important**: Gemini function calling requires Java reflection, so Kotlin compilation must enable `-javaParameters` flag (already configured in `build.gradle.kts`).

### Adding New NLP Functions

1. Add method to `BotFunctions` class (parameters must use wrapper types for Gemini)
2. Create argument data class with `@Description` on properties
3. Add `GeminiFunction` enum entry
4. Add sealed class variant to `BotCommand`
5. Handle mapping in `CommandParserService.parseFunctionCall()`
6. Handle the new command type in `NlpDialogs.kt`

```kotlin
// 1. Add to BotFunctions (use wrapper types: String, Double, not primitives)
fun myNewFunction(
    arg1: String,  // NOT kotlin.String - use java.lang.String
    arg2: Double   // NOT Double? - required params should be non-null
): Unit = Unit

// 2. Define arguments in domain/model/nlp/arguments/
data class MyNewFunctionArgs(
    @Description("Field description")
    val arg1: String,
    val arg2: Double
)

// 3. Add to GeminiFunction enum
enum class GeminiFunction {
    MY_NEW_FUNCTION;

    companion object {
        fun from(name: String?): GeminiFunction? = when (name) {
            "myNewFunction" -> MY_NEW_FUNCTION
            // ...
        }
    }
}

// 4. Add to BotCommand sealed class
sealed class BotCommand {
    data class MyNewCommand(val arg1: String, val arg2: Double) : BotCommand()
}

// 5. Handle in CommandParserService.parseFunctionCall()
GeminiFunction.MY_NEW_FUNCTION -> {
    val dto = argsMapper.map<MyNewFunctionArgs>(args)
    BotCommand.MyNewCommand(dto.arg1, dto.arg2)
}

// 6. Handle in NlpDialogs.kt processNlpCommand()
is BotCommand.MyNewCommand -> {
    // Set context and determine next state
}
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
5. Test NLP: Send natural language like "Создай группу Семья" and verify the bot parses and confirms
6. Check MongoDB: Verify data persisted correctly in `money_manager_user`, `money_group`, and `category` collections

## Upcoming Features

See `docs/SPEC_FINANCE_TRACKING.md` for detailed specification of planned finance tracking features:

- **Balance Management**: Initial balance setup, group-wide balance tracking
- **Transaction Recording**: Add expenses/incomes with categories, amounts, dates, descriptions
- **Date Selection**: Quick buttons (today/yesterday/day before) + calendar component
- **Transaction History**: View, filter, and paginate transactions by type, period, category
- **Transaction Editing**: Edit or delete individual transactions or in bulk
- **NLP Integration**: Parse transaction commands from text/voice (e.g., "Потратил 5000 на продукты вчера")

These features will add new states, entities (Transaction), and extend the NLP capabilities.

## Code Style Guidelines

- Use Kotlin idioms: data classes, extension functions, null-safety (`?.`, `?:`)
- Prefer immutability: use `val` over `var`, immutable collections
- Service methods should be small and focused (Single Responsibility)
- Avoid business logic in Dialog/Reply builders - delegate to Services
- Use meaningful names: `createGroupTransition()` not `transition1()`
- Enum naming: `UPPER_SNAKE_CASE` for states/button types, `PascalCase` for classes