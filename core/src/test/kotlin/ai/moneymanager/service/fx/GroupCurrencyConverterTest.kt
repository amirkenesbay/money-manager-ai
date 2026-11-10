package ai.moneymanager.service.fx

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.ExchangeRate
import ai.moneymanager.domain.model.ForeignAmount
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.LoanRepository
import ai.moneymanager.repository.RecurringPaymentRepository
import ai.moneymanager.repository.SettlementRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.LoanEntity
import ai.moneymanager.repository.entity.MoneyGroupEntity
import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.model.GroupType
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyList
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private val TODAY: LocalDate = LocalDate.of(2026, 11, 10)
private val OCTOBER: LocalDate = LocalDate.of(2026, 10, 3)
private val USD_TODAY = BigDecimal("484.3")
private val USD_OCTOBER = BigDecimal("478.9")
private val EUR_OCTOBER = BigDecimal("512.3")

class GroupCurrencyConverterTest {
    private val rates = mock(ExchangeRateService::class.java)
    private val operations = mock(FinanceOperationRepository::class.java)
    private val categories = mock(CategoryRepository::class.java)
    private val recurring = mock(RecurringPaymentRepository::class.java)
    private val loans = mock(LoanRepository::class.java)
    private val settlements = mock(SettlementRepository::class.java)
    private val clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val converter = GroupCurrencyConverter(rates, operations, categories, recurring, loans, settlements, clock)
    private val groupId = ObjectId()
    private val group = MoneyGroupEntity(id = groupId, name = "Личное", inviteToken = "T", ownerId = 1, memberIds = setOf(1), type = GroupType.PERSONAL, currency = Currency.KZT, initialBalance = BigDecimal("9686"))

    private fun operation(amount: String, date: LocalDate = OCTOBER, original: ForeignAmount? = null) = FinanceOperationEntity(
        id = ObjectId(), groupId = groupId, creatorId = 1, type = CategoryType.EXPENSE, amount = BigDecimal(amount),
        categoryId = ObjectId(), categoryName = "Продукты", operationDate = date, original = original,
    )

    private fun rate(from: Currency, to: Currency, date: LocalDate, value: BigDecimal) {
        `when`(rates.rate(from, to, date)).thenReturn(ExchangeRate(from, to, value, date))
    }

    @BeforeEach
    fun givenRates() {
        rate(Currency.KZT, Currency.USD, TODAY, BigDecimal.ONE.divide(USD_TODAY, RATE_SCALE, java.math.RoundingMode.HALF_UP))
        rate(Currency.KZT, Currency.USD, OCTOBER, BigDecimal.ONE.divide(USD_OCTOBER, RATE_SCALE, java.math.RoundingMode.HALF_UP))
        rate(Currency.EUR, Currency.USD, OCTOBER, EUR_OCTOBER.divide(USD_OCTOBER, RATE_SCALE, java.math.RoundingMode.HALF_UP))
        rate(Currency.USD, Currency.KZT, TODAY, USD_TODAY)
    }

    @Suppress("UNCHECKED_CAST")
    private fun savedOperations(): List<FinanceOperationEntity> {
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<FinanceOperationEntity>>
        verify(operations).saveAll(captor.capture())
        return captor.value
    }

    @Test
    fun `converts each operation by the rate of its day and keeps what was typed`() {
        `when`(operations.findByGroupId(groupId)).thenReturn(listOf(operation("12400")))

        val converted = converter.convert(group, Currency.USD)

        val saved = savedOperations().single()
        assertThat(saved.amount).isEqualByComparingTo("25.89")
        assertThat(saved.original).isEqualTo(ForeignAmount(BigDecimal("12400"), Currency.KZT))
        assertThat(saved.recorded).isEqualTo(ForeignAmount(BigDecimal("12400"), Currency.KZT))
        assertThat(converted?.currency).isEqualTo(Currency.USD)
        assertThat(converted?.initialBalance).isEqualByComparingTo("20")
    }

    @Test
    fun `converts a foreign expense from the currency it was typed in`() {
        `when`(operations.findByGroupId(groupId)).thenReturn(listOf(operation("52160", original = ForeignAmount(BigDecimal("100"), Currency.EUR))))

        converter.convert(group, Currency.USD)

        assertThat(savedOperations().single().amount).isEqualByComparingTo("106.97")
    }

    @Test
    fun `going back restores the exact amounts including a custom rate`() {
        val typedInEuros = operation("53000", original = ForeignAmount(BigDecimal("100"), Currency.EUR))
            .copy(amount = BigDecimal("106.97"), recorded = ForeignAmount(BigDecimal("53000"), Currency.KZT))
        val typedInTenge = operation("25.89", original = ForeignAmount(BigDecimal("12400"), Currency.KZT))
            .copy(recorded = ForeignAmount(BigDecimal("12400"), Currency.KZT))
        `when`(operations.findByGroupId(groupId)).thenReturn(listOf(typedInEuros, typedInTenge))

        converter.convert(group.copy(currency = Currency.USD), Currency.KZT)

        val saved = savedOperations()
        assertThat(saved[0].amount).isEqualByComparingTo("53000")
        assertThat(saved[0].original).isEqualTo(ForeignAmount(BigDecimal("100"), Currency.EUR))
        assertThat(saved[1].amount).isEqualByComparingTo("12400")
        assertThat(saved[1].original).isNull()
    }

    @Test
    fun `converts loans at today's rate`() {
        `when`(loans.findByGroupId(groupId)).thenReturn(listOf(LoanEntity(groupId = groupId, creatorId = 1, direction = LoanDirection.LENT, person = "Ерлан", amount = BigDecimal("48430"), lentOn = OCTOBER)))

        converter.convert(group, Currency.USD)

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<LoanEntity>>
        verify(loans).saveAll(captor.capture())
        assertThat(captor.value.single().amount).isEqualByComparingTo("100")
    }

    @Test
    fun `changes nothing when a rate is missing`() {
        `when`(operations.findByGroupId(groupId)).thenReturn(listOf(operation("100", original = ForeignAmount(BigDecimal("5"), Currency.TMT))))

        assertThat(converter.convert(group, Currency.USD)).isNull()
        verify(operations, never()).saveAll(anyList())
    }
}
