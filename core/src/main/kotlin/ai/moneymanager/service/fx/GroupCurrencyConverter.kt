package ai.moneymanager.service.fx

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.ForeignAmount
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.LoanRepository
import ai.moneymanager.repository.RecurringPaymentRepository
import ai.moneymanager.repository.SettlementRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.MoneyGroupEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.Executors

private const val PREFETCH_THREADS = 6

private class MissingRate : RuntimeException()

@Service
class GroupCurrencyConverter(
    private val exchangeRateService: ExchangeRateService,
    private val operationRepository: FinanceOperationRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringPaymentRepository,
    private val loanRepository: LoanRepository,
    private val settlementRepository: SettlementRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun convert(group: MoneyGroupEntity, to: Currency): MoneyGroupEntity? {
        val from = group.currency
        if (from == to) return group
        val groupId = checkNotNull(group.id)
        val today = LocalDate.now(clock)
        val operations = operationRepository.findByGroupId(groupId)
        prefetch(operations.map { it.operationDate } + today)
        return try {
            val todayRate = rateOrFail(from, to, today)
            val converted = operations.map { converted(it, from, to) }
            val categories = categoryRepository.findByGroupIdAndMonthlyLimitIsNotNull(groupId).map { it.copy(monthlyLimit = it.monthlyLimit?.let { limit -> limit.atRate(todayRate) }) }
            val recurring = recurringRepository.findByGroupIdOrderByNextDateAsc(groupId).map { it.copy(amount = it.amount.atRate(todayRate)) }
            val loans = loanRepository.findByGroupId(groupId).map { loan ->
                loan.copy(amount = loan.amount.atRate(todayRate), repayments = loan.repayments.map { it.copy(amount = it.amount.atRate(todayRate)) })
            }
            val settlements = settlementRepository.findByGroupIdOrderBySettledOnDesc(groupId).map { it.copy(amount = it.amount.atRate(todayRate)) }
            operationRepository.saveAll(converted)
            categoryRepository.saveAll(categories)
            recurringRepository.saveAll(recurring)
            loanRepository.saveAll(loans)
            settlementRepository.saveAll(settlements)
            log.info("Group currency converted: groupId={}, {} -> {}, operations={}", groupId, from, to, converted.size)
            group.copy(currency = to, initialBalance = group.initialBalance.atRate(todayRate))
        } catch (missing: MissingRate) {
            null
        }
    }

    private fun converted(operation: FinanceOperationEntity, from: Currency, to: Currency): FinanceOperationEntity {
        val recorded = operation.recorded ?: ForeignAmount(operation.amount, from)
        val source = operation.original ?: ForeignAmount(operation.amount, from)
        val amount = when (to) {
            recorded.currency -> recorded.amount
            source.currency -> source.amount
            else -> source.amount.atRate(rateOrFail(source.currency, to, operation.operationDate))
        }
        return operation.copy(amount = amount, original = source.takeIf { it.currency != to }, recorded = recorded)
    }

    private fun prefetch(dates: Collection<LocalDate>) {
        Executors.newFixedThreadPool(PREFETCH_THREADS).use { pool ->
            dates.distinct().forEach { date -> pool.submit { exchangeRateService.preload(date) } }
        }
    }

    private fun rateOrFail(from: Currency, to: Currency, date: LocalDate): BigDecimal =
        exchangeRateService.rate(from, to, date)?.rate ?: throw MissingRate().also { log.warn("No rate to convert group currency: {} -> {} on {}", from, to, date) }

}
