package ai.moneymanager.domain.model

import org.bson.types.ObjectId
import java.math.BigDecimal

data class MoneyGroup(
    val id: ObjectId? = null,
    val name: String,
    val inviteToken: String,
    val ownerId: Long,
    val memberIds: Set<Long>,
    val type: GroupType,
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val currency: Currency = Currency.DEFAULT
)

enum class GroupType {
    PERSONAL,

    SHARED
}
