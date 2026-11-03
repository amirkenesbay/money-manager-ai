package ai.moneymanager.service

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
    private val service = FinanceOperationService(repository) { }

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

        val replaced = service.replace(groupId, operationId, draft)

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

        assertThat(service.replace(groupId, operationId, draft)).isNull()
    }
}
