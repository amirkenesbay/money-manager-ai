package ai.moneymanager.repository.entity

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import java.time.LocalDateTime

private const val ENTRY_TTL_SECONDS = 172_800 // 2 дня — хватает на день использования + запас от смещения таймзон

@Document(collection = "ai_rate_limit")
@CompoundIndex(def = "{'telegramUserId': 1, 'date': 1}", unique = true)
data class AiRateLimitEntity(
    val id: ObjectId? = null,
    val telegramUserId: Long,
    val date: LocalDate,
    val count: Int,
    @Indexed(expireAfterSeconds = ENTRY_TTL_SECONDS)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
