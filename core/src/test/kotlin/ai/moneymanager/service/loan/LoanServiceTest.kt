package ai.moneymanager.service.loan

import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.model.loan.LoanDraft
import ai.moneymanager.repository.LoanRepository
import ai.moneymanager.repository.entity.LoanEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

private const val ME = 1L
private val TODAY: LocalDate = LocalDate.of(2026, 11, 5)

class LoanServiceTest {
    private val groupId = ObjectId()
    private val stored = mutableListOf<LoanEntity>()
    private val repository = mock(LoanRepository::class.java)
    private val service = LoanService(repository)

    @BeforeEach
    fun givenStore() {
        `when`(repository.save(any(LoanEntity::class.java))).thenAnswer { invocation ->
            val loan = invocation.getArgument<LoanEntity>(0).let { if (it.id == null) it.copy(id = ObjectId()) else it }
            stored.removeIf { it.id == loan.id }
            stored.add(loan)
            loan
        }
        `when`(repository.findByGroupIdAndCreatorId(groupId, ME)).thenAnswer { stored.toList() }
        `when`(repository.findByGroupIdAndCreatorIdAndId(any(ObjectId::class.java) ?: groupId, anyLong(), any(ObjectId::class.java) ?: groupId))
            .thenAnswer { invocation -> stored.firstOrNull { it.id == invocation.getArgument(2) } }
        `when`(repository.findById(any(ObjectId::class.java) ?: groupId)).thenAnswer { invocation -> Optional.ofNullable(stored.firstOrNull { it.id == invocation.getArgument(0) }) }
    }

    @Test
    fun `takes repayments in parts and closes the loan when all is back`() {
        val loan = lend("Ерлан", "20000")

        val partly = service.repay(groupId, ME, checkNotNull(loan.id), BigDecimal("5000"), TODAY)
        val closed = service.repay(groupId, ME, checkNotNull(loan.id), null, TODAY)

        assertThat(partly?.remaining).isEqualByComparingTo("15000")
        assertThat(closed?.open).isFalse()
        assertThat(closed?.repaid).isEqualByComparingTo("20000")
    }

    @Test
    fun `never takes back more than is left`() {
        val loan = lend("Ерлан", "3000")

        val repaid = service.repay(groupId, ME, checkNotNull(loan.id), BigDecimal("9000"), TODAY)

        assertThat(repaid?.repaid).isEqualByComparingTo("3000")
    }

    @Test
    fun `finds the oldest open loan of a person by name`() {
        val older = lend("Ерлан", "1000", TODAY.minusDays(10))
        lend("Ерлан", "2000", TODAY.minusDays(2))

        val repaid = service.repayPerson(groupId, ME, LoanDirection.LENT, "ерлан", BigDecimal("500"), TODAY)

        assertThat(repaid?.id).isEqualTo(older.id)
    }

    @Test
    fun `moves the due date a week ahead`() {
        val loan = service.create(groupId, ME, LoanDraft(LoanDirection.BORROWED, "Мама", BigDecimal("50000"), TODAY, TODAY.minusDays(1), null))

        assertThat(service.postpone(checkNotNull(loan.id), ME, TODAY)?.dueDate).isEqualTo(TODAY.plusDays(7))
    }

    @Test
    fun `moves money out of the balance when lent and back when borrowed`() {
        assertThat(lend("Ерлан", "20000").cashEffect).isEqualByComparingTo("-20000")
        val borrowed = service.create(groupId, ME, LoanDraft(LoanDirection.BORROWED, "Мама", BigDecimal("50000"), TODAY, null, null))
        assertThat(borrowed.cashEffect).isEqualByComparingTo("50000")
    }

    private fun lend(person: String, amount: String, date: LocalDate = TODAY) =
        service.create(groupId, ME, LoanDraft(LoanDirection.LENT, person, BigDecimal(amount), date, null, null))
}
