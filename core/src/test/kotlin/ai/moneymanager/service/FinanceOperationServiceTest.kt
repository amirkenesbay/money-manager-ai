package ai.moneymanager.service

import ai.moneymanager.domain.event.CategoryCorrected
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.OperationDraft
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate

private const val CREATOR_ID = 42L

class FinanceOperationServiceTest {
    private val groupId = ObjectId()
    private val operationId = ObjectId()
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId)
    private val repository = mock(FinanceOperationRepository::class.java)
    private val publishedEvents = mutableListOf<Any>()
    private val service = FinanceOperationService(repository) { publishedEvents.add(it) }

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
}
