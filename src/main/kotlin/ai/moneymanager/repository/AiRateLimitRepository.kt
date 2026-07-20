package ai.moneymanager.repository

import ai.moneymanager.repository.entity.AiRateLimitEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AiRateLimitRepository : MongoRepository<AiRateLimitEntity, ObjectId> {
    fun findByTelegramUserIdAndDate(telegramUserId: Long, date: LocalDate): AiRateLimitEntity?
}
