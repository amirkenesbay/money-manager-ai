package ai.moneymanager.domain.event

import ai.moneymanager.domain.model.debt.Settlement
import org.bson.types.ObjectId
import java.math.BigDecimal

data class DebtReminderRequested(
    val groupId: ObjectId,
    val creditorId: Long,
    val debtorId: Long,
    val amount: BigDecimal,
)

data class SettlementRecorded(
    val groupId: ObjectId,
    val actorId: Long,
    val settlement: Settlement,
)
