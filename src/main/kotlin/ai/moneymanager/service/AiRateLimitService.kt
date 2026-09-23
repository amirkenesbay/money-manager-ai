package ai.moneymanager.service

import ai.moneymanager.repository.AiRateLimitRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

sealed class AiRateLimitResult {
    object Allowed : AiRateLimitResult()
    data class Exceeded(val limit: Int, val resetInSeconds: Long) : AiRateLimitResult()
}

@Service
class AiRateLimitService(
    private val aiRateLimitRepository: AiRateLimitRepository,
    @Value("\${ai.rate-limit.daily-requests-per-user:10}")
    val freeDailyLimit: Int,
    @Value("\${ai.rate-limit.daily-requests-per-user-paid:100}")
    val paidDailyLimit: Int
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun tryConsume(telegramUserId: Long, hasPaidSubscription: Boolean): AiRateLimitResult {
        val limit = if (hasPaidSubscription) paidDailyLimit else freeDailyLimit
        val updated = aiRateLimitRepository.incrementAndGet(telegramUserId, LocalDate.now())

        if (updated.count > limit) {
            log.info("AI rate limit exceeded: userId=$telegramUserId, count=${updated.count}, limit=$limit")
            return AiRateLimitResult.Exceeded(limit, secondsUntilMidnight())
        }
        return AiRateLimitResult.Allowed
    }

    private fun secondsUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        return Duration.between(now, midnight).seconds
    }
}
