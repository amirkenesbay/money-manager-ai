package ai.moneymanager.service

import ai.moneymanager.domain.event.CategoryDeleted
import ai.moneymanager.domain.event.CategoryUpdated
import ai.moneymanager.domain.model.BudgetLimitResult
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.CategoryUpdateResult
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.entity.CategoryEntity
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional

private const val ACTOR_ID = 42L
private const val FREE_BUDGETS = 3
private val TAXI_LIMIT = BigDecimal("20000")

class CategoryServiceTest {
    private val groupId = ObjectId()
    private val taxi = CategoryEntity(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId)
    private val transport = CategoryEntity(ObjectId(), "Транспорт", "🚗", CategoryType.EXPENSE, groupId)
    private val repository = mock(CategoryRepository::class.java)
    private val publishedEvents = mutableListOf<Any>()
    private val service = CategoryService(repository, testLocalizationService()) { publishedEvents.add(it) }

    @BeforeEach
    fun givenCategories() {
        `when`(repository.findById(taxi.id!!)).thenReturn(Optional.of(taxi))
        `when`(repository.findByGroupIdAndNameAndType(groupId, "Транспорт", CategoryType.EXPENSE)).thenReturn(transport)
        `when`(repository.save(any(CategoryEntity::class.java))).thenAnswer { it.getArgument<CategoryEntity>(0) }
    }

    @Test
    fun `renames a category and tells the group who did it`() {
        val result = service.updateCategory(ACTOR_ID, taxi.id!!, "Такси и каршеринг", "🚕")

        assertThat((result as CategoryUpdateResult.Updated).category.name).isEqualTo("Такси и каршеринг")
        assertThat(publishedEvents).containsExactly(CategoryUpdated(groupId, ACTOR_ID, "🚕", "Такси", "🚕", "Такси и каршеринг"))
    }

    @Test
    fun `sets a monthly limit on an expense category`() {
        `when`(repository.findByGroupIdAndMonthlyLimitIsNotNull(groupId)).thenReturn(emptyList())

        val result = service.setMonthlyLimit(taxi.id!!, TAXI_LIMIT, false, FREE_BUDGETS)

        assertThat((result as BudgetLimitResult.Updated).category.monthlyLimit).isEqualTo(TAXI_LIMIT)
    }

    @Test
    fun `stops a free user at the budget limit but lets them change an existing one`() {
        val others = List(FREE_BUDGETS) { CategoryEntity(ObjectId(), "К$it", null, CategoryType.EXPENSE, groupId, BigDecimal.TEN) }
        `when`(repository.findByGroupIdAndMonthlyLimitIsNotNull(groupId)).thenReturn(others)

        assertThat(service.setMonthlyLimit(taxi.id!!, TAXI_LIMIT, false, FREE_BUDGETS)).isEqualTo(BudgetLimitResult.LimitReached(FREE_BUDGETS))
        assertThat(service.setMonthlyLimit(taxi.id!!, null, false, FREE_BUDGETS)).isInstanceOf(BudgetLimitResult.Updated::class.java)
        assertThat(service.setMonthlyLimit(taxi.id!!, TAXI_LIMIT, false, null)).isInstanceOf(BudgetLimitResult.Updated::class.java)
    }

    @Test
    fun `keeps limits off income categories`() {
        val salary = CategoryEntity(ObjectId(), "Зарплата", "💰", CategoryType.INCOME, groupId)
        `when`(repository.findById(salary.id!!)).thenReturn(Optional.of(salary))

        assertThat(service.setMonthlyLimit(salary.id!!, TAXI_LIMIT, false, FREE_BUDGETS)).isEqualTo(BudgetLimitResult.NotExpense)
    }

    @Test
    fun `refuses a name another category of the same type already has`() {
        val result = service.updateCategory(ACTOR_ID, taxi.id!!, "Транспорт", "🚕")

        assertThat(result).isEqualTo(CategoryUpdateResult.Duplicate)
        assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `tells the group when only the icon changes`() {
        val result = service.updateCategory(ACTOR_ID, taxi.id!!, "Такси", "🚖")

        assertThat((result as CategoryUpdateResult.Updated).category.icon).isEqualTo("🚖")
        assertThat(publishedEvents).containsExactly(CategoryUpdated(groupId, ACTOR_ID, "🚕", "Такси", "🚖", "Такси"))
    }

    @Test
    fun `deletes a category and tells the group who did it`() {
        assertThat(service.deleteCategory(ACTOR_ID, taxi.id!!)).isTrue()

        assertThat(publishedEvents).containsExactly(CategoryDeleted(groupId, taxi.id!!, ACTOR_ID, "🚕", "Такси"))
    }

    @Test
    fun `reports a missing category on delete`() {
        `when`(repository.findById(transport.id!!)).thenReturn(Optional.empty())

        assertThat(service.deleteCategory(ACTOR_ID, transport.id!!)).isFalse()
        assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `keeps the order the user dragged categories into`() {
        `when`(repository.findByGroupIdOrderByAuditInfoCreatedAtAsc(groupId)).thenReturn(listOf(taxi, transport))
        val saved = mutableListOf<CategoryEntity>()
        `when`(repository.saveAll(anyList<CategoryEntity>())).thenAnswer { invocation -> invocation.getArgument<List<CategoryEntity>>(0).also(saved::addAll) }

        assertThat(service.reorder(groupId, listOf(checkNotNull(transport.id), checkNotNull(taxi.id)))).isTrue()
        `when`(repository.findByGroupIdOrderByAuditInfoCreatedAtAsc(groupId)).thenReturn(saved)

        assertThat(service.getCategoriesByGroup(groupId).map { it.name }).containsExactly("Транспорт", "Такси")
        assertThat(service.reorder(groupId, listOf(ObjectId()))).isFalse()
    }
}
