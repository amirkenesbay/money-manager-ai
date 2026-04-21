package ai.moneymanager.service

import ai.moneymanager.config.TelegramFileProperties
import kz.rmr.chatmachinist.ChatMachinistProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.objects.Voice
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

sealed class VoiceValidationResult {
    object Valid : VoiceValidationResult()
    data class Invalid(val reason: String) : VoiceValidationResult()
}

@Service
class TelegramFileService(
    private val chatMachinistProperties: ChatMachinistProperties,
    private val fileProperties: TelegramFileProperties
) : DefaultAbsSender(DefaultBotOptions(), chatMachinistProperties.bot.token) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun sendTyping(chatId: Long) {
        runCatching {
            val action = SendChatAction().apply {
                setChatId(chatId)
                setAction(ActionType.TYPING)
            }
            execute(action)
        }.onFailure { log.warn("Failed to send typing action: ${it.message}") }
    }

    fun downloadVoice(voice: Voice): ByteArray? {
        val validationResult = validateVoice(voice)
        if (validationResult is VoiceValidationResult.Invalid) {
            log.warn("Voice validation failed: ${validationResult.reason}")
            return null
        }

        return try {
            log.info("Downloading voice: fileId=${voice.fileId}, duration=${voice.duration}s, size=${voice.fileSize} bytes")

            val filePath = getFilePath(voice.fileId)
            val audioBytes = fetchFileBytes(filePath)

            log.info("Voice downloaded: ${audioBytes.size} bytes")
            audioBytes
        } catch (e: Exception) {
            log.error("Failed to download voice: ${e.message}", e)
            null
        }
    }

    private fun validateVoice(voice: Voice): VoiceValidationResult {
        if (voice.duration > fileProperties.maxVoiceDurationSeconds) {
            return VoiceValidationResult.Invalid(
                "Duration ${voice.duration}s exceeds maximum ${fileProperties.maxVoiceDurationSeconds}s"
            )
        }

        if (voice.fileSize != null && voice.fileSize > fileProperties.maxFileSizeBytes) {
            val sizeMB = voice.fileSize / (1024.0 * 1024.0)
            val maxMB = fileProperties.maxFileSizeBytes / (1024.0 * 1024.0)
            return VoiceValidationResult.Invalid(
                "Size %.2f MB exceeds maximum %.2f MB".format(sizeMB, maxMB)
            )
        }

        return VoiceValidationResult.Valid
    }

    private fun getFilePath(fileId: String): String {
        val getFile = GetFile().apply {
            this.fileId = fileId
        }
        val file = execute(getFile)
        return file.filePath
    }

    private fun fetchFileBytes(filePath: String): ByteArray {
        val fileUrl = buildFileUrl(filePath)
        return URI(fileUrl).toURL().readBytes()
    }

    private fun buildFileUrl(filePath: String): String {
        return "${fileProperties.apiUrl}${chatMachinistProperties.bot.token}/$filePath"
    }
}
