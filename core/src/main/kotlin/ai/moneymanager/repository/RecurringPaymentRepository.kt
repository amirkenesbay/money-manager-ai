package ai.moneymanager.repository

import ai.moneymanager.repository.entity.RecurringPaymentEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.LocalDate

interface RecurringPaymentRepository : MongoRepository<RecurringPaymentEntity, ObjectId> {
    fun findByGroupIdOrderByNextDateAsc(groupId: ObjectId): List<RecurringPaymentEntity>
    fun findByGroupIdAndId(groupId: ObjectId, id: ObjectId): RecurringPaymentEntity?
    fun countByCreatorIdAndActiveTrue(creatorId: Long): Long
    fun findByActiveTrueAndNextDateLessThanEqual(date: LocalDate): List<RecurringPaymentEntity>
}
