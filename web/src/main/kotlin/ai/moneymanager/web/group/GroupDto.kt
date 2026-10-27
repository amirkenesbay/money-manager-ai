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

data class CreateGroupRequest(
    val name: String?,
    val currency: Currency,
)

data class UpdateGroupRequest(
    val name: String?,
    val currency: Currency?,
)

data class InitialBalanceRequest(
    val amount: BigDecimal,
)

data class MemberResponse(
    val userId: Long,
    val name: String,
    val isOwner: Boolean,
)

data class InviteResponse(
    val inviteToken: String,
    val url: String,
)
