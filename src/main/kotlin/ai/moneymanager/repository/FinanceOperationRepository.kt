package ai.moneymanager.repository

import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface FinanceOperationRepository : MongoRepository<FinanceOperationEntity, ObjectId> {
    // Spring Data `Between` генерирует строгие $gt/$lt — граничные даты (сегодня, 1-е число) выпадали.
    @Query(
        value = "{ 'groupId': ?0, 'operationDate': { '\$gte': ?1, '\$lte': ?2 } }",
        sort = "{ 'operationDate': -1 }"
    )
    fun findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FinanceOperationEntity>

    fun findByGroupId(groupId: ObjectId): List<FinanceOperationEntity>

    fun findByGroupIdOrderByOperationDateDescAuditInfoCreatedAtDesc(
        groupId: ObjectId,
        pageable: Pageable
    ): List<FinanceOperationEntity>
}
