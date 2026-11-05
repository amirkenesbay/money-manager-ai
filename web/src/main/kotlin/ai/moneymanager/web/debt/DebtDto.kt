package ai.moneymanager.web.debt

import ai.moneymanager.domain.model.debt.DebtItemKind
import ai.moneymanager.web.loan.LoanResponse
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

data class DebtsResponse(
    val balance: BigDecimal,
    val owedToMe: BigDecimal,
    val iOwe: BigDecimal,
    val mine: List<PersonalDebtResponse>,
    val others: List<TransferResponse>,
    val history: List<SettlementResponse>,
    val loans: List<LoanResponse>,
)

data class PersonalDebtResponse(
    val userId: Long,
    val name: String,
    val amount: BigDecimal,
    val items: List<DebtItemResponse>,
)

data class DebtItemResponse(
    val kind: DebtItemKind,
    val amount: BigDecimal,
    val date: LocalDate?,
    val operationId: ObjectId?,
    val icon: String?,
    val title: String?,
)

data class TransferResponse(
    val fromId: Long,
    val fromName: String,
    val toId: Long,
    val toName: String,
    val amount: BigDecimal,
)

data class SettlementResponse(
    val id: ObjectId,
    val fromId: Long,
    val fromName: String,
    val toId: Long,
    val toName: String,
    val amount: BigDecimal,
    val date: LocalDate,
    val canUndo: Boolean,
)

data class SettlementRequest(
    val fromId: Long,
    val toId: Long,
    val amount: BigDecimal,
)

data class ReminderRequest(
    val userId: Long,
)
