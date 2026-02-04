package ai.moneymanager.repository

import ai.moneymanager.repository.entity.NotificationEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationRepository : MongoRepository<NotificationEntity, ObjectId> {
    fun findByUserId(userId: Long): List<NotificationEntity>
    fun deleteByUserId(userId: Long): Long
    fun findByNextTriggerTimeBefore(time: LocalDateTime): List<NotificationEntity>
}