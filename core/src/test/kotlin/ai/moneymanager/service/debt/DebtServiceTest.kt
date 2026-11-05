package ai.moneymanager.service.debt

import ai.moneymanager.domain.event.DebtReminderRequested
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.ExpenseShare
import ai.moneymanager.domain.model.debt.DebtItemKind
import ai.moneymanager.domain.model.debt.Transfer
import ai.moneymanager.repository.SettlementRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.SettlementEntity
import ai.moneymanager.testing.InMemoryOperationRepository
import ai.moneymanager.testing.testGroup
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private const val ME = 1L
private const val ANYA = 2L
private const val DANYA = 3L
private val TODAY: LocalDate = LocalDate.of(2026, 11, 5)

class DebtServiceTest {
    private val group = testGroup(members = setOf(ME, ANYA, DANYA))
    private val groupId = checkNotNull(group.id)
    private val operations = InMemoryOperationRepository()
    private val settlements = mutableListOf<SettlementEntity>()
    private val settlementRepository = mock(SettlementRepository::class.java)
    private val events = mutableListOf<Any>()
    private val clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val service = DebtService(operations, settlementRepository, { events.add(it) }, clock)

    @BeforeEach
    fun givenSettlementStore() {
        `when`(settlementRepository.findByGroupIdOrderBySettledOnDesc(groupId)).thenAnswer { settlements.sortedByDescending { it.settledOn } }
        `when`(settlementRepository.save(any(SettlementEntity::class.java))).thenAnswer {
            it.getArgument<SettlementEntity>(0).copy(id = ObjectId()).also(settlements::add)
        }
    }

    @Test
    fun `gives the odd tenge to the payer so parts add up to the amount`() {
        val parts = partsOf(BigDecimal("100"), ExpenseShare(ANYA, setOf(ME, ANYA, DANYA)))

        assertThat(parts[ME]).isEqualByComparingTo("33.33")
        assertThat(parts[ANYA]).isEqualByComparingTo("33.34")
        assertThat(parts.values.sumOf { it }).isEqualByComparingTo("100")
    }

    @Test
    fun `settles a circle of debts with fewer transfers`() {
        val transfers = transfersOf(mapOf(ME to BigDecimal("8500"), ANYA to BigDecimal("-12000"), DANYA to BigDecimal("3500")))

        assertThat(transfers).containsExactly(Transfer(ANYA, ME, BigDecimal("8500")), Transfer(ANYA, DANYA, BigDecimal("3500")))
    }

    @Test
    fun `nets debts out and explains the rest as an offset`() {
        operations.operations = listOf(
            expense("3000", payer = ME, people = setOf(ME, ANYA, DANYA), day = 2),
            expense("1500", payer = ANYA, people = setOf(ME, ANYA, DANYA), day = 3),
        )

        val debts = service.debts(group, ME)

        assertThat(debts.balance).isEqualByComparingTo("1500")
        val danya = debts.mine.single()
        assertThat(danya.userId).isEqualTo(DANYA)
        assertThat(danya.items.map { it.kind to it.amount.toPlainString() })
            .containsExactly(DebtItemKind.EXPENSE to "1000.00", DebtItemKind.OFFSET to "500.00")
    }

    @Test
    fun `explains a debt moved through someone else as an offset`() {
        operations.operations = listOf(
            expense("2000", payer = ME, people = setOf(ANYA), day = 1),
            expense("2000", payer = ANYA, people = setOf(DANYA), day = 2),
        )

        val danya = service.debts(group, ME).mine.single()

        assertThat(danya.userId).isEqualTo(DANYA)
        assertThat(danya.items.single().kind).isEqualTo(DebtItemKind.OFFSET)
        assertThat(danya.items.single().amount).isEqualByComparingTo("2000")
    }

    @Test
    fun `a partial repayment leaves the rest and lands in the history`() {
        operations.operations = listOf(expense("7000", payer = DANYA, people = setOf(ME, DANYA), day = 3))

        val settlement = service.settle(group, ME, ME, DANYA, BigDecimal("2000"), TODAY)
        val debts = service.debts(group, ME)

        assertThat(settlement).isNotNull
        assertThat(debts.balance).isEqualByComparingTo("-1500")
        assertThat(debts.history.single().amount).isEqualByComparingTo("2000")
    }

    @Test
    fun `only the two people of a repayment can record it`() {
        assertThat(service.settle(group, ANYA, ME, DANYA, BigDecimal("1000"), TODAY)).isNull()
    }

    @Test
    fun `reminds a debtor once a day`() {
        operations.operations = listOf(expense("4000", payer = ME, people = setOf(ME, ANYA), day = 3))

        assertThat(service.remind(group, ME, ANYA)).isEqualTo(ReminderResult.SENT)
        assertThat(service.remind(group, ME, ANYA)).isEqualTo(ReminderResult.TOO_SOON)
        assertThat(service.remind(group, ME, DANYA)).isEqualTo(ReminderResult.NO_DEBT)
        assertThat(events.filterIsInstance<DebtReminderRequested>().single().amount).isEqualByComparingTo("2000")
    }

    private fun expense(amount: String, payer: Long, people: Set<Long>, day: Int) = FinanceOperationEntity(
        id = ObjectId(), groupId = groupId, creatorId = payer, type = CategoryType.EXPENSE, amount = BigDecimal(amount),
        categoryId = ObjectId(), categoryName = "Кафе", categoryIcon = "🍔", operationDate = LocalDate.of(2026, 11, day),
        payerId = payer, participantIds = people,
    )
}
