package ai.moneymanager.service.debt

import ai.moneymanager.domain.event.DebtReminderRequested
import ai.moneymanager.domain.event.SettlementRecorded
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.domain.model.debt.DebtItem
import ai.moneymanager.domain.model.debt.DebtItemKind
import ai.moneymanager.domain.model.debt.GroupDebts
import ai.moneymanager.domain.model.debt.PersonalDebt
import ai.moneymanager.domain.model.debt.Settlement
import ai.moneymanager.domain.model.debt.Transfer
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.SettlementRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.SettlementEntity
import org.bson.types.ObjectId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

private const val MAX_ITEMS = 20
private val REMIND_INTERVAL: Duration = Duration.ofDays(1)

enum class ReminderResult {
    SENT,
    TOO_SOON,
    NO_DEBT,
}

private data class Ledger(
    val operations: List<FinanceOperationEntity>,
    val settlements: List<SettlementEntity>,
) {
    fun transfers(): List<Transfer> {
        val nets = mutableMapOf<Long, BigDecimal>()
        fun add(userId: Long, amount: BigDecimal) = nets.merge(userId, amount, BigDecimal::add)
        operations.forEach { operation ->
            val share = checkNotNull(operation.share)
            add(share.payerId, operation.amount)
            partsOf(operation.amount, share).forEach { (userId, part) -> add(userId, part.negate()) }
        }
        settlements.forEach {
            add(it.fromId, it.amount)
            add(it.toId, it.amount.negate())
        }
        return transfersOf(nets)
    }
}

@Service
class DebtService(
    private val financeOperationRepository: FinanceOperationRepository,
    private val settlementRepository: SettlementRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    private val reminders = ConcurrentHashMap<String, Instant>()

    fun debts(group: MoneyGroup, userId: Long): GroupDebts {
        val ledger = ledgerOf(group)
        val transfers = ledger.transfers()
        val mine = transfers.filter { it.involves(userId) }.map { personal(userId, it, ledger) }
        return GroupDebts(
            balance = mine.fold(BigDecimal.ZERO) { total, debt -> total + debt.amount },
            mine = mine,
            others = transfers.filterNot { it.involves(userId) },
            history = ledger.settlements.map { it.toModel() },
        )
    }

    fun settle(group: MoneyGroup, actorId: Long, fromId: Long, toId: Long, amount: BigDecimal, today: LocalDate): Settlement? {
        require(amount.signum() > 0) { "A settlement moves a positive amount" }
        val valid = actorId in setOf(fromId, toId) && fromId != toId && group.memberIds.containsAll(setOf(fromId, toId))
        if (!valid) return null
        val saved = settlementRepository.save(SettlementEntity(groupId = checkNotNull(group.id), fromId = fromId, toId = toId, amount = amount, settledOn = today, creatorId = actorId))
        val settlement = saved.toModel()
        eventPublisher.publishEvent(SettlementRecorded(checkNotNull(group.id), actorId, settlement))
        return settlement
    }

    fun settleTransfer(group: MoneyGroup, debtorId: Long, creditorId: Long, today: LocalDate): Settlement? {
        val transfer = ledgerOf(group).transfers().firstOrNull { it.fromId == debtorId && it.toId == creditorId } ?: return null
        return settle(group, debtorId, debtorId, creditorId, transfer.amount, today)
    }

    fun undo(group: MoneyGroup, actorId: Long, settlementId: ObjectId): Boolean {
        val settlement = settlementRepository.findByGroupIdAndId(checkNotNull(group.id), settlementId) ?: return false
        if (settlement.creatorId != actorId) return false
        settlementRepository.delete(settlement)
        return true
    }

    fun remind(group: MoneyGroup, creditorId: Long, debtorId: Long): ReminderResult {
        val groupId = checkNotNull(group.id)
        val transfer = ledgerOf(group).transfers().firstOrNull { it.fromId == debtorId && it.toId == creditorId } ?: return ReminderResult.NO_DEBT
        val key = "$groupId:$creditorId:$debtorId"
        val now = clock.instant()
        val last = reminders[key]
        if (last != null && last.plus(REMIND_INTERVAL).isAfter(now)) return ReminderResult.TOO_SOON
        reminders[key] = now
        eventPublisher.publishEvent(DebtReminderRequested(groupId, creditorId, debtorId, transfer.amount))
        return ReminderResult.SENT
    }

    private fun ledgerOf(group: MoneyGroup): Ledger {
        val groupId = checkNotNull(group.id)
        val operations = financeOperationRepository.findAll(OperationFilter(groupId, type = CategoryType.EXPENSE)).filter { it.share != null }
        return Ledger(operations, settlementRepository.findByGroupIdOrderBySettledOnDesc(groupId))
    }

    private fun personal(userId: Long, transfer: Transfer, ledger: Ledger): PersonalDebt {
        val owedToMe = transfer.toId == userId
        val otherId = if (owedToMe) transfer.fromId else transfer.toId
        val amount = if (owedToMe) transfer.amount else transfer.amount.negate()
        val pairItems = (expenseItems(userId, otherId, ledger.operations) + settlementItems(userId, otherId, ledger.settlements))
            .sortedByDescending { it.date }
        val shown = pairItems.take(MAX_ITEMS)
        val earlier = pairItems.drop(MAX_ITEMS).sumOf { it.amount }
        val offset = amount - pairItems.sumOf { it.amount }
        val items = shown +
            listOfNotNull(
                DebtItem(DebtItemKind.EARLIER, earlier).takeIf { earlier.signum() != 0 },
                DebtItem(DebtItemKind.OFFSET, offset).takeIf { offset.signum() != 0 },
            )
        return PersonalDebt(otherId, amount, items)
    }

    private fun expenseItems(userId: Long, otherId: Long, operations: List<FinanceOperationEntity>): List<DebtItem> =
        operations.mapNotNull { operation ->
            val share = checkNotNull(operation.share)
            val parts = partsOf(operation.amount, share)
            val amount = when (share.payerId) {
                userId -> parts[otherId]
                otherId -> parts[userId]?.negate()
                else -> null
            } ?: return@mapNotNull null
            DebtItem(DebtItemKind.EXPENSE, amount, operation.operationDate, operation.id, operation.categoryIcon, operation.description ?: operation.categoryName)
        }

    private fun settlementItems(userId: Long, otherId: Long, settlements: List<SettlementEntity>): List<DebtItem> =
        settlements.mapNotNull { settlement ->
            val amount = when {
                settlement.fromId == otherId && settlement.toId == userId -> settlement.amount.negate()
                settlement.fromId == userId && settlement.toId == otherId -> settlement.amount
                else -> null
            } ?: return@mapNotNull null
            DebtItem(DebtItemKind.SETTLEMENT, amount, settlement.settledOn)
        }

    private fun Transfer.involves(userId: Long): Boolean = fromId == userId || toId == userId

    private fun SettlementEntity.toModel() = Settlement(checkNotNull(id), fromId, toId, amount, settledOn, creatorId)
}
