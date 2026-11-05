package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Recurrence
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.RecurringDraft
import ai.moneymanager.domain.model.RecurringMode
import ai.moneymanager.domain.model.RecurringSaveResult
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.RecurringPaymentRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.RecurringPaymentEntity
import ai.moneymanager.testing.testFinanceOperationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate

private const val OWNER_ID = 42L
private const val FREE_RULES = 3
private val TODAY: LocalDate = LocalDate.of(2026, 11, 15)

class RecurringPaymentServiceTest {
    private val groupId = ObjectId()
    private val subscriptions = Category(ObjectId(), "Подписки", "📺", CategoryType.EXPENSE, groupId)
    private val repository = mock(RecurringPaymentRepository::class.java)
    private val operations = mock(FinanceOperationRepository::class.java)
    private val categoryService = mock(CategoryService::class.java)
    private val service = RecurringPaymentService(repository, testFinanceOperationService(operations), categoryService)
    private val draft = RecurringDraft(
        CategoryType.EXPENSE, BigDecimal("3500"), subscriptions, "Netflix", Recurrence(RecurrenceUnit.MONTH), LocalDate.of(2026, 10, 15), null, RecurringMode.AUTO,
    )

    @BeforeEach
    fun givenRepositoriesEchoSaves() {
        `when`(repository.save(any(RecurringPaymentEntity::class.java))).thenAnswer { it.getArgument<RecurringPaymentEntity>(0) }
        `when`(operations.save(any(FinanceOperationEntity::class.java))).thenAnswer { it.getArgument<FinanceOperationEntity>(0).copy(id = ObjectId()) }
        `when`(categoryService.getCategory(checkNotNull(subscriptions.id))).thenReturn(subscriptions)
    }

    @Test
    fun `starts from the next date when the first payment is already recorded`() {
        val saved = service.create(OWNER_ID, groupId, draft.copy(startDate = TODAY), firstRecorded = true, FREE_RULES, TODAY)

        assertThat((saved as RecurringSaveResult.Saved).payment.nextDate).isEqualTo(LocalDate.of(2026, 12, 15))
    }

    @Test
    fun `does not let a free user keep more active payments than allowed`() {
        `when`(repository.countByCreatorIdAndActiveTrue(OWNER_ID)).thenReturn(FREE_RULES.toLong())

        assertThat(service.create(OWNER_ID, groupId, draft, firstRecorded = false, FREE_RULES, TODAY)).isEqualTo(RecurringSaveResult.LimitReached(FREE_RULES))
    }

    @Test
    fun `records every missed date once and moves on`() {
        val payment = payment(nextDate = LocalDate.of(2026, 9, 15))

        val recorded = service.recordDue(payment, TODAY)

        assertThat(recorded.map { it.date }).containsExactly(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), TODAY)
        assertThat(recorded.map { it.operation?.operationDate }).containsExactly(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15), TODAY)
        assertThat(savedPayment().nextDate).isEqualTo(LocalDate.of(2026, 12, 15))
    }

    @Test
    fun `only reminds when the payment is not automatic`() {
        val recorded = service.recordDue(payment(nextDate = TODAY, mode = RecurringMode.REMIND), TODAY)

        assertThat(recorded.single().operation).isNull()
    }

    @Test
    fun `stops after the end date`() {
        service.recordDue(payment(nextDate = TODAY, endDate = LocalDate.of(2026, 12, 1)), TODAY)

        assertThat(savedPayment().active).isFalse()
    }

    @Test
    fun `warns once the day before`() {
        val tomorrow = TODAY.plusDays(1)
        val payment = payment(nextDate = tomorrow)

        assertThat(service.claimHeadsUp(payment, tomorrow)).isTrue()
        assertThat(service.claimHeadsUp(payment.copy(headsUpSentFor = tomorrow), tomorrow)).isFalse()
        assertThat(service.claimHeadsUp(payment, TODAY)).isFalse()
    }

    private fun payment(nextDate: LocalDate, mode: RecurringMode = RecurringMode.AUTO, endDate: LocalDate? = null) = RecurringPaymentEntity(
        id = ObjectId(), groupId = groupId, creatorId = OWNER_ID, type = CategoryType.EXPENSE, amount = BigDecimal("3500"),
        categoryId = checkNotNull(subscriptions.id), categoryName = subscriptions.name, categoryIcon = subscriptions.icon, description = "Netflix",
        unit = RecurrenceUnit.MONTH, interval = 1, startDate = LocalDate.of(2026, 9, 15), endDate = endDate, nextDate = nextDate, mode = mode,
    )

    private fun savedPayment(): RecurringPaymentEntity {
        val captor = ArgumentCaptor.forClass(RecurringPaymentEntity::class.java)
        verify(repository).save(captor.capture())
        return captor.value
    }
}
