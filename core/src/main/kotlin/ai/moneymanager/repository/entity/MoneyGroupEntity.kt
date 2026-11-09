package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.CURRENT_DEFAULTS_VERSION
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.DEFAULT_MONTH_START_DAY
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.SplitDefault
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "money_group")
data class MoneyGroupEntity(
    val id: ObjectId? = null,
    val name: String,
    @Indexed(unique = true)
    val inviteToken: String,
    val ownerId: Long,
    val memberIds: Set<Long>,
    val type: GroupType,
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val currency: Currency = Currency.DEFAULT,
    val monthStartDay: Int = DEFAULT_MONTH_START_DAY,
    val splitExpenses: Boolean = false,
    val splitDefault: SplitDefault = SplitDefault.MINE,
    val defaultCategoriesVersion: Int = CURRENT_DEFAULTS_VERSION,
    val auditInfo: AuditInfo = AuditInfo()
)
