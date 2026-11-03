package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class OperationsChanged(
    val groupId: ObjectId,
)
