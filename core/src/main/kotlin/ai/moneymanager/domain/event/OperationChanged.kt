package ai.moneymanager.domain.event

import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId

data class OperationChanged(
    val groupId: ObjectId,
    val actorId: Long,
    val change: OperationChange,
)

sealed interface OperationChange {
    val operationId: ObjectId

    data class Added(val operation: FinanceOperationEntity) : OperationChange {
        override val operationId: ObjectId get() = checkNotNull(operation.id)
    }

    data class Edited(val before: FinanceOperationEntity, val after: FinanceOperationEntity) : OperationChange {
        override val operationId: ObjectId get() = checkNotNull(after.id)
    }

    data class Deleted(val operation: FinanceOperationEntity) : OperationChange {
        override val operationId: ObjectId get() = checkNotNull(operation.id)
    }
}

fun FinanceOperationEntity.sameContentAs(other: FinanceOperationEntity): Boolean = copy(auditInfo = other.auditInfo) == other
