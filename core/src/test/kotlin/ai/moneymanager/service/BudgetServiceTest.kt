package ai.moneymanager.service

import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.BudgetAlertRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.BudgetAlertEntity
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.dao.DuplicateKeyException
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private const val SALARY_DAY = 25
private val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
private val PERIOD_START: LocalDate = LocalDate.of(2026, 9, SALARY_DAY)
private val PERIOD_END: LocalDate = LocalDate.of(2026, 10, SALARY_DAY - 1)

class BudgetServiceTest {
    private val groupId = ObjectId()
    private val group = testGroup(groupId).copy(monthStartDay = SALARY_DAY)
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId, BigDecimal("20000"))
    private val categoryService = mock(CategoryService::class.java)
    private val operations = mock(FinanceOperationRepository::class.java)
    private val alerts = mock(BudgetAlertRepository::class.java)
    private val claimed = mutableSetOf<Pair<ObjectId, BudgetThreshold>>()
    private val clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val budgetService = BudgetService(categoryService, operations, clock)
    private val alertService = BudgetAlertService(budgetService, alerts)

    @BeforeEach
    fun givenBudgets() {
        `when`(categoryService.getBudgetCategories(groupId)).thenReturn(listOf(taxi))
        `when`(alerts.insert(any(BudgetAlertEntity::class.java))).thenAnswer {
            val alert = it.getArgument<BudgetAlertEntity>(0)
            if (!claimed.add(alert.categoryId to alert.threshold)) throw DuplicateKeyException("E11000")
            alert
        }
    }

    @Test
    fun `counts only expenses of the category inside the group month`() {
        givenSpent(expense(12000), expense(5000), income(100000))

        val budget = budgetService.budgets(group).single()

        assertThat(budget.spent).isEqualByComparingTo("17000")
        assertThat(budget.remaining).isEqualByComparingTo("3000")
        assertThat(budget.percent).isEqualTo(85)
        assertThat(budget.period.start).isEqualTo(PERIOD_START)
    }

    @Test
    fun `warns once at 80 percent and once more over the limit`() {
        givenSpent(expense(16000))
        assertThat(alertService.claimNewAlerts(group).map { it.threshold }).containsExactly(BudgetThreshold.NEAR)

        givenSpent(expense(17000))
        assertThat(alertService.claimNewAlerts(group)).isEmpty()

        givenSpent(expense(21000))
        assertThat(alertService.claimNewAlerts(group).map { it.threshold }).containsExactly(BudgetThreshold.OVER)
    }

    @Test
    fun `sends only the over-limit warning when one expense jumps past both thresholds`() {
        givenSpent(expense(25000))

        assertThat(alertService.claimNewAlerts(group).map { it.threshold }).containsExactly(BudgetThreshold.OVER)
        assertThat(claimed.map { it.second }).containsExactlyInAnyOrder(BudgetThreshold.NEAR, BudgetThreshold.OVER)
    }

    @Test
    fun `adds what was left last month to a rolling budget`() {
        `when`(categoryService.getBudgetCategories(groupId)).thenReturn(listOf(taxi.copy(rollover = true)))
        givenSpent(expense(18000))
        givenSpentBefore(expense(15000))

        val budget = budgetService.budgets(group).single()

        assertThat(budget.carriedOver).isEqualByComparingTo("5000")
        assertThat(budget.remaining).isEqualByComparingTo("7000")
        assertThat(budget.percent).isEqualTo(72)
    }

    @Test
    fun `takes last month overspending off a rolling budget`() {
        `when`(categoryService.getBudgetCategories(groupId)).thenReturn(listOf(taxi.copy(rollover = true)))
        givenSpent(expense(10000))
        givenSpentBefore(expense(26000))

        val budget = budgetService.budgets(group).single()

        assertThat(budget.carriedOver).isEqualByComparingTo("-6000")
        assertThat(budget.remaining).isEqualByComparingTo("4000")
        assertThat(budget.percent).isEqualTo(71)
    }

    @Test
    fun `treats a budget eaten up by last month as already over`() {
        `when`(categoryService.getBudgetCategories(groupId)).thenReturn(listOf(taxi.copy(rollover = true)))
        givenSpent(expense(1000))
        givenSpentBefore(expense(45000))

        assertThat(budgetService.budgets(group).single().percent).isEqualTo(BudgetThreshold.OVER.percent)
    }

    private fun givenSpentBefore(vararg spent: FinanceOperationEntity) {
        `when`(operations.findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, PERIOD_START.minusMonths(1), PERIOD_START.minusDays(1)))
            .thenReturn(spent.toList())
    }

    private fun givenSpent(vararg spent: FinanceOperationEntity) {
        `when`(operations.findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(groupId, PERIOD_START, PERIOD_END)).thenReturn(spent.toList())
    }

    private fun expense(amount: Int) = operation(CategoryType.EXPENSE, amount)

    private fun income(amount: Int) = operation(CategoryType.INCOME, amount)

    private fun operation(type: CategoryType, amount: Int) = FinanceOperationEntity(
        groupId = groupId,
        creatorId = TEST_USER_ID,
        type = type,
        amount = BigDecimal(amount),
        categoryId = checkNotNull(taxi.id),
        categoryName = taxi.name,
        operationDate = TODAY,
    )
}
