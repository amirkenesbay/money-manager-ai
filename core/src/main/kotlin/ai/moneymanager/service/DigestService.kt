package ai.moneymanager.service

import ai.moneymanager.domain.model.Digest
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.DigestPeriod
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.MonthLeft
import ai.moneymanager.domain.model.currentMonthPeriod
import ai.moneymanager.domain.model.daysAfter
import ai.moneymanager.domain.model.periodStartingAt
import ai.moneymanager.domain.model.report.HistoryReport
import ai.moneymanager.repository.DigestSentRepository
import ai.moneymanager.repository.entity.DigestSentEntity
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private const val TOP_EXPENSES_LIMIT = 4

@Service
class DigestService(
    private val financeHistoryService: FinanceHistoryService,
    private val budgetService: BudgetService,
    private val digestSentRepository: DigestSentRepository,
) {
    fun wasSent(telegramUserId: Long, period: DigestPeriod): Boolean =
        digestSentRepository.existsByTelegramUserIdAndKindAndPeriodStart(telegramUserId, period.kind, period.start)

    fun claim(telegramUserId: Long, period: DigestPeriod): Boolean = try {
        digestSentRepository.insert(DigestSentEntity(telegramUserId = telegramUserId, kind = period.kind, periodStart = period.start))
        true
    } catch (exception: DuplicateKeyException) {
        false
    }

    fun build(group: MoneyGroup, period: DigestPeriod, today: LocalDate): Digest? {
        val report = history(group, period.start, period.end)
        if (report.operations.isEmpty()) return null
        val isMonth = period.kind == DigestKind.MONTH
        return Digest(
            period = period,
            group = group,
            spent = report.totalExpense,
            income = report.totalIncome,
            operationCount = report.operations.size,
            topExpenses = report.expenseByCategory.take(TOP_EXPENSES_LIMIT),
            previousSpent = if (isMonth) previousSpent(group, period) else null,
            budgets = if (isMonth) budgetService.budgets(group, periodStartingAt(period.start)) else emptyList(),
            monthLeft = if (isMonth) null else monthLeft(group, today),
        )
    }

    private fun previousSpent(group: MoneyGroup, period: DigestPeriod): BigDecimal? {
        val previous = periodStartingAt(period.start.minusMonths(1))
        return history(group, previous.start, previous.end).totalExpense.takeIf { it.signum() > 0 }
    }

    private fun monthLeft(group: MoneyGroup, today: LocalDate): MonthLeft? {
        val month = currentMonthPeriod(today, group.monthStartDay)
        val days = daysAfter(today, month)
        val report = history(group, month.start, month.end)
        if (days <= 0 || report.totalIncome.signum() <= 0) return null
        return MonthLeft(month.month, report.balance, days)
    }

    private fun history(group: MoneyGroup, start: LocalDate, end: LocalDate): HistoryReport =
        financeHistoryService.buildHistoryReport(checkNotNull(group.id), start, end)
}
