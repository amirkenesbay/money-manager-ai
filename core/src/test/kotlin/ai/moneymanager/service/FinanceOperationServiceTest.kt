package ai.moneymanager.service

import ai.moneymanager.domain.event.CategoryCorrected
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.ExpenseShare
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.OperationDraft
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.domain.model.OperationTotals
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.MoneyGroupEntity
import ai.moneymanager.testing.testFinanceOperationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

private const val CREATOR_ID = 42L
private const val PARTNER_ID = 43L
private const val STRANGER_ID = 99L

class FinanceOperationServiceTest {
    private val groupId = ObjectId()
    private val operationId = ObjectId()
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId)
    private val repository = mock(FinanceOperationRepository::class.java)
    private val publishedEvents = mutableListOf<Any>()
    private val groupRepository = mock(MoneyGroupRepository::class.java)
    private val service = testFinanceOperationService(repository, groupRepository) { publishedEvents.add(it) }

    private val existing = FinanceOperationEntity(
        id = operationId,
        groupId = groupId,
        creatorId = CREATOR_ID,
        type = CategoryType.EXPENSE,
        amount = BigDecimal("3500"),
        categoryId = ObjectId(),
        categoryName = "Продукты",
        categoryIcon = "🛒",
        operationDate = LocalDate.of(2026, 10, 12),
        description = "Магнум",
    )

    @BeforeEach
    fun givenRepositoryEchoesSaves() {
        `when`(repository.save(any(FinanceOperationEntity::class.java))).thenAnswer { it.getArgument<FinanceOperationEntity>(0) }
    }

    @Test
    fun `creates an operation from a draft with the category snapshot`() {
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("1800"), taxi, LocalDate.of(2026, 10, 14), null)

        val created = service.create(groupId, CREATOR_ID, draft)

        assertThat(created.groupId).isEqualTo(groupId)
        assertThat(created.creatorId).isEqualTo(CREATOR_ID)
        assertThat(created.categoryId).isEqualTo(taxi.id)
        assertThat(created.categoryName).isEqualTo("Такси")
        assertThat(created.categoryIcon).isEqualTo("🚕")
        assertThat(created.amount).isEqualByComparingTo("1800")
    }

    @Test
    fun `splits a new expense equally between all members when the group shares expenses`() {
        `when`(groupRepository.findById(groupId)).thenReturn(Optional.of(splitGroup(splitExpenses = true)))

        val created = service.create(groupId, CREATOR_ID, OperationDraft(CategoryType.EXPENSE, BigDecimal("3000"), taxi, LocalDate.of(2026, 11, 4), null))

        assertThat(created.share).isEqualTo(ExpenseShare(CREATOR_ID, setOf(CREATOR_ID, PARTNER_ID)))
    }

    @Test
    fun `keeps the chosen payer and people but drops a share of strangers`() {
        `when`(groupRepository.findById(groupId)).thenReturn(Optional.of(splitGroup(splitExpenses = true)))
        val chosen = ExpenseShare(PARTNER_ID, setOf(CREATOR_ID))
        val stranger = ExpenseShare(STRANGER_ID, setOf(CREATOR_ID))

        val kept = service.create(groupId, CREATOR_ID, OperationDraft(CategoryType.EXPENSE, BigDecimal("3000"), taxi, LocalDate.of(2026, 11, 4), null, chosen))
        val dropped = service.create(groupId, CREATOR_ID, OperationDraft(CategoryType.EXPENSE, BigDecimal("3000"), taxi, LocalDate.of(2026, 11, 4), null, stranger))

        assertThat(kept.share).isEqualTo(chosen)
        assertThat(dropped.share).isEqualTo(ExpenseShare(CREATOR_ID, setOf(CREATOR_ID, PARTNER_ID)))
    }

    @Test
    fun `does not split when the group does not share expenses`() {
        `when`(groupRepository.findById(groupId)).thenReturn(Optional.of(splitGroup(splitExpenses = false)))

        val created = service.create(groupId, CREATOR_ID, OperationDraft(CategoryType.EXPENSE, BigDecimal("3000"), taxi, LocalDate.of(2026, 11, 4), null))

        assertThat(created.share).isNull()
    }

    @Test
    fun `replaces the fields but keeps the identity and the creator`() {
        `when`(repository.findByGroupIdAndIdIn(groupId, listOf(operationId))).thenReturn(listOf(existing))
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("1800"), taxi, LocalDate.of(2026, 10, 14), null)

        val replaced = service.replace(CREATOR_ID, groupId, operationId, draft)

        assertThat(replaced?.id).isEqualTo(operationId)
        assertThat(replaced?.creatorId).isEqualTo(CREATOR_ID)
        assertThat(replaced?.categoryName).isEqualTo("Такси")
        assertThat(replaced?.operationDate).isEqualTo(LocalDate.of(2026, 10, 14))
        assertThat(replaced?.description).isNull()
    }

    @Test
    fun `does not replace an operation of another group`() {
        `when`(repository.findByGroupIdAndIdIn(groupId, listOf(operationId))).thenReturn(emptyList())
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("1800"), taxi, LocalDate.of(2026, 10, 14), null)

        assertThat(service.replace(CREATOR_ID, groupId, operationId, draft)).isNull()
    }

    @Test
    fun `reports a corrected category when an edit moves the operation to another category`() {
        `when`(repository.findByGroupIdAndIdIn(groupId, listOf(operationId))).thenReturn(listOf(existing))

        service.updateCategory(CREATOR_ID, groupId, operationId, checkNotNull(taxi.id), taxi.name, taxi.icon)

        assertThat(publishedEvents).contains(CategoryCorrected(groupId, "Магнум", checkNotNull(taxi.id)))
    }

    @Test
    fun `reports no correction when the category stays the same`() {
        `when`(repository.findByGroupIdAndIdIn(groupId, listOf(operationId))).thenReturn(listOf(existing))

        service.updateAmount(CREATOR_ID, groupId, operationId, BigDecimal("4000"))

        assertThat(publishedEvents).noneMatch { it is CategoryCorrected }
    }

    @Test
    fun `reports a corrected category when the user changed the suggested one`() {
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("1800"), taxi, LocalDate.of(2026, 10, 14), "Яндекс")

        service.create(groupId, CREATOR_ID, draft, categoryCorrected = true)

        assertThat(publishedEvents).contains(CategoryCorrected(groupId, "Яндекс", checkNotNull(taxi.id)))
    }

    @Test
    fun `reports no correction when the suggestion was kept`() {
        val draft = OperationDraft(CategoryType.EXPENSE, BigDecimal("1800"), taxi, LocalDate.of(2026, 10, 14), "Яндекс")

        service.create(groupId, CREATOR_ID, draft)

        assertThat(publishedEvents).noneMatch { it is CategoryCorrected }
    }

    @Test
    fun `searches in memory, pages the matches and sums them`() {
        val filter = OperationFilter(groupId, query = "магнум")
        val other = existing.copy(id = ObjectId(), description = "Small")
        val second = existing.copy(id = ObjectId(), amount = BigDecimal("1500"))
        `when`(repository.findAll(filter)).thenReturn(listOf(existing, other, second))

        val page = service.findOperations(filter, 1, 1)

        assertThat(page.content).containsExactly(second)
        assertThat(page.totalElements).isEqualTo(2)
        assertThat(service.totals(filter)).isEqualTo(OperationTotals(BigDecimal.ZERO, BigDecimal("5000"), 2))
    }

    @Test
    fun `lists recent tags of the group without repeats`() {
        val newest = existing.copy(id = ObjectId(), description = "кофе #отпуск #море")
        val older = existing.copy(id = ObjectId(), description = "отель #Отпуск #ремонт")
        `when`(repository.findAll(OperationFilter(groupId))).thenReturn(listOf(newest, older))

        assertThat(service.recentTags(groupId, 2)).containsExactly("отпуск", "море")
    }

    private fun splitGroup(splitExpenses: Boolean) = MoneyGroupEntity(
        id = groupId, name = "Алматы", inviteToken = "TOKEN", ownerId = CREATOR_ID,
        memberIds = setOf(CREATOR_ID, PARTNER_ID), type = GroupType.SHARED, splitExpenses = splitExpenses,
    )
}
