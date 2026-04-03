package ai.moneymanager.repository

import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface FinanceOperationRepository : MongoRepository<FinanceOperationEntity, ObjectId> {
    fun findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FinanceOperationEntity>
}
