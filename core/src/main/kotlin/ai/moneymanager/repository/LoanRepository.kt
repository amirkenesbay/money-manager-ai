package ai.moneymanager.repository

import ai.moneymanager.repository.entity.LoanEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.LocalDate

interface LoanRepository : MongoRepository<LoanEntity, ObjectId> {
    fun findByGroupId(groupId: ObjectId): List<LoanEntity>
    fun findByGroupIdAndCreatorId(groupId: ObjectId, creatorId: Long): List<LoanEntity>
    fun findByGroupIdAndCreatorIdAndId(groupId: ObjectId, creatorId: Long, id: ObjectId): LoanEntity?
    fun findByDueDateLessThanEqual(date: LocalDate): List<LoanEntity>
}
