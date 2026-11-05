package ai.moneymanager.testing

import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.mockito.Mockito.mock
import java.time.LocalDate

class InMemoryOperationRepository(
    private val delegate: FinanceOperationRepository = mock(FinanceOperationRepository::class.java),
) : FinanceOperationRepository by delegate {
    var operations: List<FinanceOperationEntity> = emptyList()

    override fun findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<FinanceOperationEntity> = operations.filter { it.groupId == groupId && it.operationDate in startDate..endDate }

    override fun findAll(filter: OperationFilter): List<FinanceOperationEntity> = operations.filter { operation ->
        operation.groupId == filter.groupId &&
            (filter.from == null || !operation.operationDate.isBefore(filter.from)) &&
            (filter.to == null || !operation.operationDate.isAfter(filter.to)) &&
            (filter.type == null || operation.type == filter.type) &&
            (filter.categoryId == null || operation.categoryId == filter.categoryId) &&
            (filter.creatorId == null || operation.creatorId == filter.creatorId)
    }.sortedByDescending { it.operationDate }

    override fun findByGroupId(groupId: ObjectId): List<FinanceOperationEntity> = operations.filter { it.groupId == groupId }

    override fun <S : FinanceOperationEntity> saveAll(entities: Iterable<S>): List<S> {
        val saved = entities.map { it }
        operations = operations + saved.map { it.copy(id = it.id ?: ObjectId()) }
        return saved
    }

    override fun deleteByGroupIdAndImportId(groupId: ObjectId, importId: ObjectId): Long {
        val before = operations.size
        operations = operations.filterNot { it.groupId == groupId && it.importId == importId }
        return (before - operations.size).toLong()
    }
}
