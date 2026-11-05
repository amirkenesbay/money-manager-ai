package ai.moneymanager.domain.model.debt

import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

enum class DebtItemKind {
    EXPENSE,
    SETTLEMENT,
    OFFSET,
    EARLIER,
}

data class DebtItem(
    val kind: DebtItemKind,
    val amount: BigDecimal,
    val date: LocalDate? = null,
    val operationId: ObjectId? = null,
    val icon: String? = null,
    val title: String? = null,
)

data class PersonalDebt(
    val userId: Long,
    val amount: BigDecimal,
    val items: List<DebtItem>,
)

data class Transfer(
    val fromId: Long,
    val toId: Long,
    val amount: BigDecimal,
)

data class Settlement(
    val id: ObjectId,
    val fromId: Long,
    val toId: Long,
    val amount: BigDecimal,
    val date: LocalDate,
    val creatorId: Long,
)

data class GroupDebts(
    val balance: BigDecimal,
    val mine: List<PersonalDebt>,
    val others: List<Transfer>,
    val history: List<Settlement>,
)
