package ai.moneymanager.chat.transition.nlp

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.nlp.CommandParserService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import org.telegram.telegrambots.meta.api.objects.Voice

private const val MAX_VOICE_DURATION_SECONDS = 180

private fun processTextMessage(
    userMessage: String,
    context: MoneyManagerContext,
    commandParserService: CommandParserService,
    geminiService: GeminiService
) {
    log.info("🧠 Processing NLP: $userMessage")
    val command = commandParserService.parseCommand(userMessage)
    processNlpCommand(command, context, geminiService)
}

private fun processVoiceMessage(
    voice: Voice,
    context: MoneyManagerContext,
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    geminiService: GeminiService
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
    processNlpCommand(command, context, geminiService)
    return true
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createTextInputTransition(
    sourceState: MoneyManagerState,
    commandParserService: CommandParserService,
    geminiService: GeminiService
) {
    transition {
        name = "Process text from ${sourceState.name}"

        condition {
            from = sourceState
            eventType = EventType.TEXT
            guard {
                context.isActive &&
                !context.manualTextInputActive &&
                (update.message?.text?.let { !it.startsWith("/") } ?: false)
            }
        }

        action {
            val userMessage = update.message?.text ?: return@action
            processTextMessage(userMessage, context, commandParserService, geminiService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createVoiceInputTransition(
    sourceState: MoneyManagerState,
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    geminiService: GeminiService
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
            processVoiceMessage(voice, context, commandParserService, telegramFileService, geminiService)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
            trigger { sameDialog = true }
        }
    }
}