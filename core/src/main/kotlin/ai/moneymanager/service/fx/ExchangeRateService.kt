package ai.moneymanager.service.fx

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.ExchangeRate
import ai.moneymanager.repository.KztRatesRepository
import ai.moneymanager.repository.entity.KztRatesEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

private val RETRY_AFTER: Duration = Duration.ofMinutes(30)
private const val AMOUNT_SCALE = 2

@Service
class ExchangeRateService(
    private val repository: KztRatesRepository,
    private val source: KztRatesSource,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lastAttempts = ConcurrentHashMap<LocalDate, Instant>()

    fun rate(from: Currency, to: Currency, date: LocalDate): ExchangeRate? {
        val day = minOf(date, LocalDate.now(clock))
        if (from == to) return ExchangeRate(from, to, BigDecimal.ONE, day)
        val rates = ratesOn(day) ?: return null
        val fromKzt = rates.kztPer(from) ?: return null
        val toKzt = rates.kztPer(to) ?: return null
        return ExchangeRate(from, to, fromKzt.divide(toKzt, RATE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros(), rates.date)
    }

    fun convert(amount: BigDecimal, from: Currency, to: Currency, date: LocalDate): BigDecimal? =
        rate(from, to, date)?.let { amount.multiply(it.rate).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros() }

    private fun ratesOn(day: LocalDate): KztRatesEntity? =
        repository.findById(KztRatesEntity.dayOf(day)).orElse(null)
            ?: fetched(day)
            ?: repository.findFirstByDayLessThanEqualOrderByDayDesc(KztRatesEntity.dayOf(day))

    private fun fetched(day: LocalDate): KztRatesEntity? {
        if (!claimAttempt(day)) return null
        val rates = runCatching { source.fetch(day) }
            .onFailure { log.warn("Exchange rates fetch failed: date={}, error={}", day, it.message) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        log.info("Exchange rates fetched: date={}, currencies={}", day, rates.size)
        return repository.save(KztRatesEntity(KztRatesEntity.dayOf(day), rates))
    }

    private fun claimAttempt(day: LocalDate): Boolean {
        val now = clock.instant()
        val previous = lastAttempts[day]
        if (previous != null && Duration.between(previous, now) < RETRY_AFTER) return false
        lastAttempts[day] = now
        return true
    }

    private fun KztRatesEntity.kztPer(currency: Currency): BigDecimal? =
        if (currency == Currency.KZT) BigDecimal.ONE else kztPerUnit[currency]
}
