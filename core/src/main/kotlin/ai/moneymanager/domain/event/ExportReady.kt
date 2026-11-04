package ai.moneymanager.domain.event

import java.time.LocalDate

data class ExportReady(
    val telegramUserId: Long,
    val groupName: String,
    val from: LocalDate?,
    val to: LocalDate?,
    val count: Int,
    val fileName: String,
    val content: ByteArray,
)
