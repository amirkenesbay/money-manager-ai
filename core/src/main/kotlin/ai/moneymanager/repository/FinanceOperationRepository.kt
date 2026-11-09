package ai.moneymanager.repository

import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import ai.moneymanager.domain.model.CategoryType
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface FinanceOperationRepository : MongoRepository<FinanceOperationEntity, ObjectId>, FinanceOperationRepositoryCustom {
    fun existsByCreatorIdAndAuditInfoCreatedAtGreaterThanEqual(creatorId: Long, since: LocalDateTime): Boolean
    fun findByCreatorIdAndGroupIdAndTypeAndAuditInfoCreatedAtGreaterThanEqual(creatorId: Long, groupId: ObjectId, type: CategoryType, since: LocalDateTime): List<FinanceOperationEntity>

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

    fun findByGroupIdAndIdIn(groupId: ObjectId, ids: List<ObjectId>): List<FinanceOperationEntity>

    fun deleteByGroupIdAndImportId(groupId: ObjectId, importId: ObjectId): Long

    fun findByGroupIdOrderByOperationDateDescAuditInfoCreatedAtDesc(
        groupId: ObjectId,
        pageable: Pageable
    ): List<FinanceOperationEntity>
}
