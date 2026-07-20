package ai.moneymanager.repository

import ai.moneymanager.repository.entity.AiRateLimitEntity
import java.time.LocalDate

interface AiRateLimitRepositoryCustom {
    /** Атомарно инкрементирует счётчик за указанный день (upsert), возвращает запись после инкремента. */
    fun incrementAndGet(telegramUserId: Long, date: LocalDate): AiRateLimitEntity
}
