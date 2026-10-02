package ai.moneymanager.web.group

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.GroupType
import org.bson.types.ObjectId
import java.math.BigDecimal

data class GroupResponse(
    val id: ObjectId,
    val name: String,
    val type: GroupType,
    val currency: Currency,
    val isOwner: Boolean,
    val isActive: Boolean,
    val memberCount: Int,
    val initialBalance: BigDecimal,
)
