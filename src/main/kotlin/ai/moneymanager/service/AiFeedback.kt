package ai.moneymanager.service

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private const val TYPING_HEARTBEAT_SECONDS = 4L

class AiFeedback internal constructor(
    private val chatId: Long,
    private val telegramFileService: TelegramFileService
) {
    private var placeholderMessageId: Int? = null
    private var heartbeat: ScheduledFuture<*>? = null

    internal fun start() {
        telegramFileService.sendTyping(chatId)
        heartbeat = telegramFileService.scheduler().scheduleAtFixedRate(
            { telegramFileService.sendTyping(chatId) },
            TYPING_HEARTBEAT_SECONDS,
            TYPING_HEARTBEAT_SECONDS,
            TimeUnit.SECONDS
        )
    }

    fun show(text: String) {
        val existingId = placeholderMessageId
        if (existingId == null) {
            placeholderMessageId = telegramFileService.sendPlaceholder(chatId, text)
        } else {
            telegramFileService.editPlaceholder(chatId, existingId, text)
        }
    }

    internal fun stop() {
        heartbeat?.cancel(false)
        placeholderMessageId?.let { telegramFileService.deletePlaceholder(chatId, it) }
    }
}

fun <T> TelegramFileService.withAiFeedback(chatId: Long, block: (AiFeedback) -> T): T {
    val feedback = AiFeedback(chatId, this)
    feedback.start()
    try {
        return block(feedback)
    } finally {
        feedback.stop()
    }
}
