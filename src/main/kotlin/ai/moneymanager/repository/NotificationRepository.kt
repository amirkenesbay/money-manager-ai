package ai.moneymanager.repository

import ai.moneymanager.repository.entity.NotificationEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationRepository : MongoRepository<NotificationEntity, ObjectId> {
    fun findByTelegramUserIdOrderByAuditInfoCreatedAtAsc(telegramUserId: Long): List<NotificationEntity>
    fun findByIsActiveTrueAndNextFireTimeLessThanEqual(now: LocalDateTime): List<NotificationEntity>
    fun deleteByTelegramUserId(telegramUserId: Long): Long
    fun countByTelegramUserId(telegramUserId: Long): Long
}
