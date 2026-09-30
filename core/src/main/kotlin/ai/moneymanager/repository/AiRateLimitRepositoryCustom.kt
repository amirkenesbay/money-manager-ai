package ai.moneymanager.repository

import ai.moneymanager.repository.entity.AiRateLimitEntity
import java.time.LocalDate

interface AiRateLimitRepositoryCustom {
    fun incrementAndGet(telegramUserId: Long, date: LocalDate): AiRateLimitEntity
}
