package ai.moneymanager.service.debt

import ai.moneymanager.domain.model.ExpenseShare
import ai.moneymanager.domain.model.debt.Transfer
import java.math.BigDecimal
import java.math.RoundingMode

private const val MONEY_SCALE = 2

fun partsOf(amount: BigDecimal, share: ExpenseShare): Map<Long, BigDecimal> {
    val participants = share.participantIds.sorted()
    val base = amount.divide(BigDecimal(participants.size), MONEY_SCALE, RoundingMode.DOWN)
    val rest = amount - base * BigDecimal(participants.size)
    val restTaker = share.payerId.takeIf { it in participants } ?: participants.first()
    return participants.associateWith { if (it == restTaker) base + rest else base }
}

fun transfersOf(nets: Map<Long, BigDecimal>): List<Transfer> {
    val creditors = nets.filterValues { it.signum() > 0 }.toMutableMap()
    val debtors = nets.filterValues { it.signum() < 0 }.mapValues { it.value.negate() }.toMutableMap()
    val transfers = mutableListOf<Transfer>()
    while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
        val creditor = creditors.maxBy { it.value }
        val debtor = debtors.maxBy { it.value }
        val amount = creditor.value.min(debtor.value)
        transfers += Transfer(debtor.key, creditor.key, amount)
        settle(creditors, creditor.key, amount)
        settle(debtors, debtor.key, amount)
    }
    return transfers
}

private fun settle(balances: MutableMap<Long, BigDecimal>, userId: Long, amount: BigDecimal) {
    val left = checkNotNull(balances[userId]) - amount
    if (left.signum() > 0) balances[userId] = left else balances.remove(userId)
}
