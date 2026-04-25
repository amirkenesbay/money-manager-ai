package ai.moneymanager.service

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

private const val SYSTEM_PROMPT_PATH = "prompts/ai-system-prompt.md"
private const val VOICE_TRANSCRIPTION_PROMPT_PATH = "prompts/ai-voice-transcription.md"
private const val OUT_OF_CONTEXT_PROMPT_PATH = "prompts/ai-out-of-context.md"

private const val PLACEHOLDER_USER_MESSAGE = "{userMessage}"
private const val PLACEHOLDER_REPLY_LANGUAGE = "{replyLanguage}"

@Service
class AiPromptService {

    val systemPrompt: String by lazy { loadPrompt(SYSTEM_PROMPT_PATH) }

    val voiceTranscriptionPrompt: String by lazy { loadPrompt(VOICE_TRANSCRIPTION_PROMPT_PATH) }

    private val outOfContextTemplate: String by lazy { loadPrompt(OUT_OF_CONTEXT_PROMPT_PATH) }

    fun outOfContextPrompt(userMessage: String, replyLanguage: String): String =
        outOfContextTemplate
            .replace(PLACEHOLDER_USER_MESSAGE, userMessage)
            .replace(PLACEHOLDER_REPLY_LANGUAGE, replyLanguage)

    private fun loadPrompt(path: String): String =
        ClassPathResource(path).inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8).trim() }
}
