package ai.moneymanager.domain.event

import org.bson.types.ObjectId

data class MemberJoined(
    val groupId: ObjectId,
    val memberId: Long,
)
