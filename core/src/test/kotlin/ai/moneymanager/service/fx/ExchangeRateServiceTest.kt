package ai.moneymanager.service.fx

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.repository.KztRatesRepository
import ai.moneymanager.repository.entity.KztRatesEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional

private val TODAY: LocalDate = LocalDate.of(2026, 11, 10)
private val YESTERDAY: LocalDate = TODAY.minusDays(1)
private val PUBLISHED = mapOf(Currency.USD to BigDecimal("484.3"), Currency.EUR to BigDecimal("521.6"))

class ExchangeRateServiceTest {
    private val repository = mock(KztRatesRepository::class.java)
    private val fetches = mutableListOf<LocalDate>()
    private var published: Map<Currency, BigDecimal> = PUBLISHED
    private var now: Instant = TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC)
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
    }
    private val service = ExchangeRateService(repository, { date -> fetches.add(date); published }, clock)

    @BeforeEach
    fun givenEmptyStore() {
        `when`(repository.findById(anyString())).thenReturn(Optional.empty())
        `when`(repository.save(any(KztRatesEntity::class.java))).thenAnswer { it.getArgument<KztRatesEntity>(0) }
    }

    @Test
    fun `uses stored rates without asking the bank`() {
        `when`(repository.findById(KztRatesEntity.dayOf(YESTERDAY))).thenReturn(Optional.of(KztRatesEntity(KztRatesEntity.dayOf(YESTERDAY), PUBLISHED)))

        val rate = service.rate(Currency.USD, Currency.KZT, YESTERDAY)

        assertThat(rate?.rate).isEqualByComparingTo("484.3")
        assertThat(rate?.date).isEqualTo(YESTERDAY)
        assertThat(fetches).isEmpty()
    }

    @Test
    fun `fetches and stores a missing day`() {
        val rate = service.rate(Currency.EUR, Currency.KZT, TODAY)

        assertThat(rate?.rate).isEqualByComparingTo("521.6")
        assertThat(fetches).containsExactly(TODAY)
    }

    @Test
    fun `crosses two foreign currencies through tenge`() {
        val rate = service.rate(Currency.EUR, Currency.USD, TODAY)

        assertThat(rate?.rate).isEqualByComparingTo(BigDecimal("521.6").divide(BigDecimal("484.3"), RATE_SCALE, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun `inverts a rate into tenge`() {
        assertThat(service.convert(BigDecimal("9000"), Currency.KZT, Currency.USD, TODAY)).isEqualByComparingTo("18.58")
    }

    @Test
    fun `same currency needs no rates`() {
        assertThat(service.rate(Currency.KZT, Currency.KZT, TODAY)?.rate).isEqualByComparingTo("1")
        assertThat(fetches).isEmpty()
    }

    @Test
    fun `falls back to the latest known day while today is not published`() {
        published = emptyMap()
        `when`(repository.findFirstByDayLessThanEqualOrderByDayDesc(KztRatesEntity.dayOf(TODAY)))
            .thenReturn(KztRatesEntity(KztRatesEntity.dayOf(YESTERDAY), PUBLISHED))

        val rate = service.rate(Currency.USD, Currency.KZT, TODAY)

        assertThat(rate?.date).isEqualTo(YESTERDAY)
    }

    @Test
    fun `asks the bank again only after the retry pause`() {
        published = emptyMap()

        service.rate(Currency.USD, Currency.KZT, TODAY)
        service.rate(Currency.USD, Currency.KZT, TODAY)
        now = now.plus(Duration.ofMinutes(31))
        service.rate(Currency.USD, Currency.KZT, TODAY)

        assertThat(fetches).hasSize(2)
    }

    @Test
    fun `survives a bank outage`() {
        val failing = ExchangeRateService(repository, { error("timeout") }, clock)

        assertThat(failing.rate(Currency.USD, Currency.KZT, TODAY)).isNull()
    }

    @Test
    fun `treats a future date as today`() {
        service.rate(Currency.USD, Currency.KZT, TODAY.plusDays(3))

        assertThat(fetches).containsExactly(TODAY)
    }

    @Test
    fun `returns nothing for a currency the bank does not publish`() {
        assertThat(service.rate(Currency.TMT, Currency.KZT, TODAY)).isNull()
    }
}
