package ai.moneymanager.service

import ai.moneymanager.repository.entity.AiRateLimitEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

sealed class AiRateLimitResult {
    object Allowed : AiRateLimitResult()
    data class Exceeded(val limit: Int, val resetInSeconds: Long) : AiRateLimitResult()
}

/**
 * Дневная квота AI-запросов на юзера. Хранится в Mongo (не in-memory), чтобы переживать
 * рестарт бота и работать одинаково при масштабировании на несколько инстансов.
 */
@Service
class AiRateLimitService(
    private val mongoTemplate: MongoTemplate,
    @Value("\${ai.rate-limit.daily-requests-per-user:10}")
    private val dailyLimit: Int
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /** Атомарно инкрементирует счётчик за сегодня и проверяет лимит. Вызывать ПЕРЕД запросом к Gemini. */
    fun tryConsume(telegramUserId: Long): AiRateLimitResult {
        val today = LocalDate.now()
        val query = Query(
            Criteria.where("telegramUserId").`is`(telegramUserId).and("date").`is`(today)
        )
        val update = Update().inc("count", 1).setOnInsert("createdAt", LocalDateTime.now())
        val options = FindAndModifyOptions.options().upsert(true).returnNew(true)

        val updated = mongoTemplate.findAndModify(query, update, options, AiRateLimitEntity::class.java)
            ?: return AiRateLimitResult.Allowed // findAndModify не должен вернуть null при upsert=true, но подстрахуемся

        if (updated.count > dailyLimit) {
            log.info("AI rate limit exceeded: userId=$telegramUserId, count=${updated.count}, limit=$dailyLimit")
            return AiRateLimitResult.Exceeded(dailyLimit, secondsUntilMidnight())
        }
        return AiRateLimitResult.Allowed
    }

    private fun secondsUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        return Duration.between(now, midnight).seconds
    }
}
