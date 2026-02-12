package ai.moneymanager.chat.dialog

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.nlp.CommandParserService
import gcardone.junidecode.Junidecode
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Voice

private val log = LoggerFactory.getLogger("NlpDialogs")

// ========== Helper Functions ==========

/**
 * Проверка совпадения названий с учетом транслитерации
 * Использует библиотеку Junidecode для транслитерации в ASCII
 */
private fun matchesGroupName(groupName: String, searchName: String): Boolean {
    val normalizedGroupName = groupName.lowercase()
    val normalizedSearchName = searchName.lowercase()

    // Точное совпадение
    if (normalizedGroupName == normalizedSearchName) return true

    // Транслитерация обеих строк в латиницу и сравнение
    // "тест" -> "test", "test" -> "test", "семья" -> "sem'a"
    val translitGroupName = Junidecode.unidecode(normalizedGroupName).lowercase()
    val translitSearchName = Junidecode.unidecode(normalizedSearchName).lowercase()

    return translitGroupName == translitSearchName
}

// ========== Extension Properties ==========

/**
 * Возвращает целевое состояние для команды NLP
 */
private val BotCommand.targetState: MoneyManagerState
    get() = when (this) {
        is BotCommand.CreateGroup -> MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
        is BotCommand.DeleteGroup -> MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
        is BotCommand.OutOfContext,
        is BotCommand.AddExpense,
        is BotCommand.AddIncome,
        is BotCommand.ParseError -> MoneyManagerState.NLP_RESPONSE
    }

// ========== Constants ==========

private const val OUT_OF_CONTEXT_MESSAGE = """Я бот для учета финансов. Могу помочь:
• Создать группу ("создай группу друзья")
• Добавить расход ("кофе 500")
• Добавить доход ("зарплата 500000")"""

private const val VOICE_TOO_LONG_MESSAGE = "⚠️ Голосовое сообщение слишком длинное (%dс). Максимум 3 минуты."
private const val VOICE_DOWNLOAD_ERROR_MESSAGE = "❌ Не удалось загрузить голосовое сообщение. Попробуйте еще раз."
private const val PARSE_ERROR_MESSAGE = "Не удалось обработать сообщение. Попробуйте еще раз."

private const val MAX_VOICE_DURATION_SECONDS = 180

// ========== Command Processing ==========

/**
 * Обрабатывает команду NLP и заполняет контекст
 */
private fun processNlpCommand(
    command: BotCommand,
    context: MoneyManagerContext
) {
    clearNlpContext(context)
    context.parsedCommand = command

    when (command) {
        is BotCommand.CreateGroup -> handleCreateGroupCommand(command, context)
        is BotCommand.DeleteGroup -> handleDeleteGroupCommand(command, context)
        is BotCommand.OutOfContext -> handleOutOfContextCommand(context)
        is BotCommand.AddExpense -> handleAddExpenseCommand(command, context)
        is BotCommand.AddIncome -> handleAddIncomeCommand(command, context)
        is BotCommand.ParseError -> handleParseErrorCommand(command, context)
    }
}

private fun clearNlpContext(context: MoneyManagerContext) {
    context.nlpResponse = null
    context.nlpGroupName = null
    context.nlpTargetState = null
    context.nlpGroupToDelete = null
}

private fun handleCreateGroupCommand(command: BotCommand.CreateGroup, context: MoneyManagerContext) {
    context.nlpGroupName = command.groupName
    context.nlpTargetState = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
    log.info("✅ NLP parsed: CreateGroup(${command.groupName})")
}

private fun handleDeleteGroupCommand(command: BotCommand.DeleteGroup, context: MoneyManagerContext) {
    context.nlpGroupName = command.groupName
    context.nlpTargetState = MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
    log.info("✅ NLP parsed: DeleteGroup(${command.groupName})")
}

private fun handleOutOfContextCommand(context: MoneyManagerContext) {
    context.nlpResponse = OUT_OF_CONTEXT_MESSAGE
    context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
    log.info("⚠️ NLP: Out of context message")
}

private fun handleAddExpenseCommand(command: BotCommand.AddExpense, context: MoneyManagerContext) {
    context.nlpResponse = """
        |Функция добавления расходов скоро будет доступна!
        |Распознано: ${command.category ?: "без категории"}, ${command.amount} тг
    """.trimMargin()
    context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
    log.info("✅ NLP parsed: AddExpense(${command.amount}, ${command.category})")
}

private fun handleAddIncomeCommand(command: BotCommand.AddIncome, context: MoneyManagerContext) {
    context.nlpResponse = """
        |Функция добавления доходов скоро будет доступна!
        |Распознано: ${command.category ?: "без категории"}, ${command.amount} тг
    """.trimMargin()
    context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
    log.info("✅ NLP parsed: AddIncome(${command.amount}, ${command.category})")
}

private fun handleParseErrorCommand(command: BotCommand.ParseError, context: MoneyManagerContext) {
    context.nlpResponse = PARSE_ERROR_MESSAGE
    context.nlpTargetState = MoneyManagerState.NLP_RESPONSE
    log.info("❌ NLP error: ${command.error}")
}

/**
 * Обрабатывает текстовое сообщение через NLP
 */
private fun processTextMessage(
    userMessage: String,
    context: MoneyManagerContext,
    commandParserService: CommandParserService
) {
    log.info("🧠 Processing NLP: $userMessage")
    val command = commandParserService.parseCommand(userMessage)
    processNlpCommand(command, context)
}

/**
 * Обрабатывает голосовое сообщение через NLP
 */
private fun processVoiceMessage(
    voice: Voice,
    context: MoneyManagerContext,
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService
): Boolean {
    log.info("🎤 Processing voice message: ${voice.duration}s")

    val audioBytes = telegramFileService.downloadVoice(voice)
    if (audioBytes == null) {
        context.nlpResponse = if (voice.duration > MAX_VOICE_DURATION_SECONDS) {
            VOICE_TOO_LONG_MESSAGE.format(voice.duration)
        } else {
            VOICE_DOWNLOAD_ERROR_MESSAGE
        }
        return false
    }

    val command = commandParserService.parseVoiceCommand(audioBytes)
    processNlpCommand(command, context)
    return true
}

// ========== Transition Builders ==========

/**
 * Создает transition для обработки текстовых сообщений
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createTextInputTransition(
    sourceState: MoneyManagerState,
    commandParserService: CommandParserService
) {
    transition {
        name = "Process text from ${sourceState.name}"

        condition {
            from = sourceState
            eventType = EventType.TEXT
            guard {
                update.message?.text?.let { !it.startsWith("/") } ?: false
            }
        }

        action {
            val userMessage = update.message?.text ?: return@action
            processTextMessage(userMessage, context, commandParserService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }
}

/**
 * Создает transition для обработки голосовых сообщений
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createVoiceInputTransition(
    sourceState: MoneyManagerState,
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService
) {
    transition {
        name = "Process voice from ${sourceState.name}"

        condition {
            from = sourceState
            eventType = EventType.VOICE
        }

        action {
            val voice = update.message?.voice ?: return@action
            log.info("🎤 Processing voice from ${sourceState.name}: ${voice.duration}s")
            processVoiceMessage(voice, context, commandParserService, telegramFileService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }
}

/**
 * Создает transition для роутинга NLP команд
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createNlpRouter(
    sourceState: MoneyManagerState,
    targetState: MoneyManagerState
) {
    transition {
        name = "Route NLP: ${sourceState.name} → ${targetState.name}"

        condition {
            from = sourceState
            eventType = EventType.TRIGGERED
            guard {
                val matches = context.parsedCommand?.targetState == targetState
                log.info("🔍 NLP Router (${sourceState.name}→${targetState.name}): command=${context.parsedCommand}, match=$matches")
                matches
            }
        }

        action {
            log.info("🎯 Routing from ${sourceState.name} to ${targetState.name}")
            context.parsedCommand = null
        }

        then {
            to = targetState
        }
    }
}

// ========== Main Dialog Builder ==========

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.nlpDialogTransitions(
    commandParserService: CommandParserService,
    groupService: GroupService,
    userInfoService: UserInfoService,
    telegramFileService: TelegramFileService
) {
    // Input transitions (текст и голос из MENU и NLP_RESPONSE)
    listOf(MoneyManagerState.MENU, MoneyManagerState.NLP_RESPONSE).forEach { state ->
        createTextInputTransition(state, commandParserService)
        createVoiceInputTransition(state, commandParserService, telegramFileService)
    }

    // NLP роутеры для всех комбинаций источников и целей
    val sourceStates = listOf(MoneyManagerState.MENU, MoneyManagerState.NLP_RESPONSE)
    val targetStates = listOf(
        MoneyManagerState.NLP_CONFIRM_CREATE_GROUP,
        MoneyManagerState.NLP_CONFIRM_DELETE_GROUP,
        MoneyManagerState.NLP_RESPONSE
    )

    sourceStates.forEach { source ->
        targetStates.forEach { target ->
            createNlpRouter(source, target)
        }
    }

    // Возврат из NLP ответа в меню
    transition {
        name = "Back to menu from NLP response"

        condition {
            from = MoneyManagerState.NLP_RESPONSE
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        action {
            context.nlpResponse = null
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    // Подтверждение создания группы через NLP
    transition {
        name = "Confirm NLP create group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            button = MoneyManagerButtonType.CONFIRM_NLP_ACTION
        }

        action {
            val groupName = context.nlpGroupName ?: return@action
            val userId = user.id

            val createdGroup = groupService.createGroup(userId, groupName)
            context.currentGroup = createdGroup
            context.userInfo = userInfoService.getUserInfo(user)
            context.nlpGroupName = null

            log.info("✅ Group created via NLP: ${createdGroup.name}")
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    // Отмена создания группы через NLP
    transition {
        name = "Cancel NLP create group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.nlpGroupName = null
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    // Подтверждение удаления группы через NLP
    transition {
        name = "Confirm NLP delete group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
            button = MoneyManagerButtonType.CONFIRM_NLP_ACTION
        }

        action {
            val groupName = context.nlpGroupName ?: return@action
            val userId = user.id
            val userInfo = context.userInfo ?: return@action

            // Находим группу по имени среди групп пользователя (с учетом транслитерации)
            val group = groupService.getUserGroups(userId).find {
                matchesGroupName(it.name, groupName)
            }

            if (group == null) {
                context.nlpResponse = "❌ Группа \"$groupName\" не найдена среди ваших групп."
                context.nlpGroupName = null
                log.info("❌ Group not found for deletion: $groupName")
                return@action
            }

            // Проверяем, является ли пользователь владельцем
            if (group.ownerId != userId) {
                context.nlpResponse = "❌ Только владелец может удалить группу \"${group.name}\"."
                context.nlpGroupName = null
                log.info("❌ User ${userId} is not owner of group ${group.name}")
                return@action
            }

            // Проверяем наличие ID группы
            val groupId = group.id ?: run {
                context.nlpResponse = "❌ Ошибка: группа не имеет ID."
                context.nlpGroupName = null
                log.info("❌ Group has no ID: ${group.name}")
                return@action
            }

            // Удаляем группу
            val deleted = groupService.deleteGroup(userId, groupId)
            context.userInfo = userInfoService.getUserInfo(user)
            context.nlpGroupName = null

            if (deleted) {
                context.nlpResponse = "✅ Группа \"${group.name}\" успешно удалена."
                log.info("✅ Group deleted via NLP: ${group.name}")
            } else {
                context.nlpResponse = "❌ Не удалось удалить группу \"${group.name}\"."
                log.info("❌ Failed to delete group: ${group.name}")
            }
        }

        then {
            to = MoneyManagerState.NLP_RESPONSE
        }
    }

    // Отмена удаления группы через NLP
    transition {
        name = "Cancel NLP delete group"

        condition {
            from = MoneyManagerState.NLP_CONFIRM_DELETE_GROUP
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.nlpGroupName = null
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}