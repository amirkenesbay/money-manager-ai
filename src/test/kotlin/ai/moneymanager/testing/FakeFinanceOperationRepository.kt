package ai.moneymanager.testing

import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.mockito.Mockito.mock
import java.time.LocalDate

class FakeFinanceOperationRepository(
    private val delegate: FinanceOperationRepository = mock(FinanceOperationRepository::class.java)
) : FinanceOperationRepository by delegate {

    var operations: List<FinanceOperationEntity> = emptyList()

    override fun findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FinanceOperationEntity> = operations
}
