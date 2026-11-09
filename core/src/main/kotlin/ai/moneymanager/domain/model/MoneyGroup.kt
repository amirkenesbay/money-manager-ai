package ai.moneymanager.domain.model

import org.bson.types.ObjectId
import java.math.BigDecimal

const val MAX_GROUP_NAME_LENGTH = 50

data class MoneyGroup(
    val id: ObjectId? = null,
    val name: String,
    val inviteToken: String,
    val ownerId: Long,
    val memberIds: Set<Long>,
    val type: GroupType,
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val currency: Currency = Currency.DEFAULT,
    val monthStartDay: Int = DEFAULT_MONTH_START_DAY,
    val splitExpenses: Boolean = false,
    val splitDefault: SplitDefault = SplitDefault.MINE,
) {
    val splitsExpenses: Boolean
        get() = splitExpenses && type == GroupType.SHARED
}

enum class SplitDefault {
    MINE,
    EVERYONE,
}

enum class GroupType {
    PERSONAL,

    SHARED
}
