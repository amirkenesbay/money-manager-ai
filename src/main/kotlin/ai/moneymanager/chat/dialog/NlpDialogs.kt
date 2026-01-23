package ai.moneymanager.chat.dialog

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.nlp.CommandParserService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import org.telegram.telegrambots.meta.api.objects.Voice

/**
 * Обрабатывает команду NLP и заполняет context
 */
private fun processNlpCommand(
    command: BotCommand,
    context: MoneyManagerContext
) {
    // Очищаем предыдущие NLP данные
    context.nlpResponse = null
    context.nlpGroupName = null

    when (command) {
        is BotCommand.CreateGroup -> {
            context.nlpGroupName = command.groupName
            println("✅ NLP parsed: CreateGroup(${command.groupName})")
        }
        is BotCommand.OutOfContext -> {
            context.nlpResponse = "Я бот для учета финансов. Могу помочь:\n" +
                    "• Создать группу (\"создай группу друзья\")\n" +
                    "• Добавить расход (\"кофе 500\")\n" +
                    "• Добавить доход (\"зарплата 500000\")"
            println("⚠️ NLP: Out of context message")
        }
        is BotCommand.AddExpense -> {
            context.nlpResponse = "Функция добавления расходов скоро будет доступна!\n" +
                    "Распознано: ${command.category ?: "без категории"}, ${command.amount} тг"
            println("✅ NLP parsed: AddExpense(${command.amount}, ${command.category})")
        }
        is BotCommand.AddIncome -> {
            context.nlpResponse = "Функция добавления доходов скоро будет доступна!\n" +
                    "Распознано: ${command.category ?: "без категории"}, ${command.amount} тг"
            println("✅ NLP parsed: AddIncome(${command.amount}, ${command.category})")
        }
        is BotCommand.ParseError -> {
            context.nlpResponse = "Не удалось обработать сообщение. Попробуйте еще раз."
            println("❌ NLP error: ${command.error}")
        }
    }
}

/**
 * Обрабатывает текстовое сообщение через NLP
 */
private fun processNlpMessage(
    userMessage: String,
    context: MoneyManagerContext,
    commandParserService: CommandParserService
) {
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
) {
    println("🎤 Processing voice message: ${voice.duration}s")

    val audioBytes = telegramFileService.downloadVoice(voice)
    if (audioBytes == null) {
        context.nlpResponse = "Не удалось загрузить голосовое сообщение. Попробуйте еще раз."
        return
    }

    val command = commandParserService.parseVoiceCommand(audioBytes)
    processNlpCommand(command, context)
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.nlpDialogTransitions(
    commandParserService: CommandParserService,
    groupService: GroupService,
    userInfoService: UserInfoService,
    telegramFileService: TelegramFileService
) {
    // Обработка произвольного текста из меню через NLP
    transition {
        name = "Process text message via NLP"

        condition {
            from = MoneyManagerState.MENU
            eventType = EventType.TEXT

            guard {
                val text = update.message?.text
                // Обрабатываем только текстовые сообщения, не команды
                text != null && !text.startsWith("/")
            }
        }

        action {
            val userMessage = update.message?.text ?: return@action
            println("🧠 Processing NLP: $userMessage")
            processNlpMessage(userMessage, context, commandParserService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Обработка текста из NLP_RESPONSE (продолжение диалога без нажатия "Назад")
    transition {
        name = "Process text message from NLP response"

        condition {
            from = MoneyManagerState.NLP_RESPONSE
            eventType = EventType.TEXT

            guard {
                val text = update.message?.text
                text != null && !text.startsWith("/")
            }
        }

        action {
            val userMessage = update.message?.text ?: return@action
            println("🧠 Processing NLP (from response): $userMessage")
            processNlpMessage(userMessage, context, commandParserService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Обработка голосового сообщения из меню
    transition {
        name = "Process voice message via NLP"

        condition {
            from = MoneyManagerState.MENU
            eventType = EventType.VOICE
        }

        action {
            val voice = update.message?.voice ?: return@action
            println("🎤 Processing voice from MENU: ${voice.duration}s")
            processVoiceMessage(voice, context, commandParserService, telegramFileService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Обработка голосового сообщения из NLP_RESPONSE
    transition {
        name = "Process voice message from NLP response"

        condition {
            from = MoneyManagerState.NLP_RESPONSE
            eventType = EventType.VOICE
        }

        action {
            val voice = update.message?.voice ?: return@action
            println("🎤 Processing voice from NLP_RESPONSE: ${voice.duration}s")
            processVoiceMessage(voice, context, commandParserService, telegramFileService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }

    // Если NLP распознал команду создания группы — показываем подтверждение
    transition {
        name = "Show NLP create group confirmation"

        condition {
            from = MoneyManagerState.MENU
            eventType = EventType.TRIGGERED

            guard {
                context.nlpGroupName != null
            }
        }

        then {
            to = MoneyManagerState.NLP_CONFIRM_CREATE_GROUP
        }
    }

    // Если NLP вернул ответ (out of context или другое) — показываем сообщение
    transition {
        name = "Show NLP response"

        condition {
            from = MoneyManagerState.MENU
            eventType = EventType.TRIGGERED

            guard {
                context.nlpResponse != null && context.nlpGroupName == null
            }
        }

        then {
            to = MoneyManagerState.NLP_RESPONSE
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

            // Обновляем userInfo
            context.userInfo = userInfoService.getUserInfo(user)

            // Очищаем NLP данные
            context.nlpGroupName = null

            println("✅ Group created via NLP: ${createdGroup?.name}")
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
}