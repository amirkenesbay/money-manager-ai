package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryCreationResult
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.imports.CategoryDecision
import ai.moneymanager.domain.model.imports.SkipReason
import ai.moneymanager.repository.ImportBatchRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.ImportBatchEntity
import ai.moneymanager.service.CategoryService
import ai.moneymanager.testing.InMemoryOperationRepository
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

private const val FILE = """date;categoryName;comment;outcome;income
2026-11-03;Кафе;капучино;1500;
2026-11-03;Продукты;Магнум;3500;
2026-11-02;Зарплата;;;450000"""
private const val FREE_IMPORTS = 3
private const val CATEGORY_LIMIT = 10

class ImportServiceTest {
    private val group = testGroup()
    private val groupId = checkNotNull(group.id)
    private val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
    private val salary = Category(ObjectId(), "Зарплата", "💰", CategoryType.INCOME, groupId)
    private val operations = InMemoryOperationRepository()
    private val batches = mutableListOf<ImportBatchEntity>()
    private val batchRepository = mock(ImportBatchRepository::class.java)
    private val categoryService = mock(CategoryService::class.java)
    private val service = ImportService(ImportDrafts(Clock.systemUTC()), mock(ImportColumnAi::class.java), operations, batchRepository, categoryService) { }

    @BeforeEach
    fun givenStores() {
        `when`(categoryService.getCategoriesByGroup(groupId)).thenReturn(listOf(groceries, salary))
        `when`(categoryService.createCategory(anyLong(), anyString(), nullable(String::class.java), any(CategoryType::class.java) ?: CategoryType.EXPENSE, any(ObjectId::class.java) ?: groupId, anyInt()))
            .thenAnswer { CategoryCreationResult.Created(Category(ObjectId(), it.getArgument(1), null, it.getArgument(3), groupId)) }
        `when`(batchRepository.save(any(ImportBatchEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<ImportBatchEntity>(0).let { if (it.id == null) it.copy(id = ObjectId()) else it }.also { saved -> batches.removeIf { it.id == saved.id }; batches.add(saved) }
        }
        `when`(batchRepository.countByUserIdAndUndoneFalse(TEST_USER_ID)).thenAnswer { batches.count { !it.undone }.toLong() }
        `when`(batchRepository.findByIdAndUserId(any(ObjectId::class.java) ?: groupId, anyLong())).thenAnswer { invocation -> batches.firstOrNull { it.id == invocation.getArgument(0) } }
    }

    @Test
    fun `sums up the file and matches categories that already exist`() {
        val preview = service.read(TEST_USER_ID, group, FILE.toByteArray())

        assertThat(preview.count).isEqualTo(3)
        assertThat(preview.from).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(preview.categories.associate { it.name to (it.existingId != null) }).containsEntry("Продукты", true).containsEntry("Кафе", false)
    }

    @Test
    fun `imports everything, creates new categories and undoes the whole batch`() {
        val token = service.read(TEST_USER_ID, group, FILE.toByteArray()).token

        val done = service.commit(token, TEST_USER_ID, group, emptyMap(), FREE_IMPORTS, CATEGORY_LIMIT) as ImportCommit.Done

        assertThat(done.result.count).isEqualTo(3)
        assertThat(done.result.createdCategories).isEqualTo(1)
        assertThat(operations.operations).allMatch { it.importId == done.result.importId }
        assertThat(service.undo(TEST_USER_ID, group, done.result.importId)).isTrue()
        assertThat(operations.operations).isEmpty()
        assertThat(service.importsUsed(TEST_USER_ID)).isZero()
    }

    @Test
    fun `skips a category on request and rows already recorded`() {
        operations.operations = listOf(operation("3500", "Магнум"))
        val preview = service.read(TEST_USER_ID, group, FILE.toByteArray())
        val cafe = preview.categories.first { it.name == "Кафе" }

        val done = service.commit(preview.token, TEST_USER_ID, group, mapOf(cafe.key to CategoryDecision.Skip), FREE_IMPORTS, CATEGORY_LIMIT) as ImportCommit.Done

        assertThat(preview.skipped).containsEntry(SkipReason.DUPLICATE, 1)
        assertThat(done.result.count).isEqualTo(1)
    }

    @Test
    fun `stops at the free number of imports`() {
        repeat(FREE_IMPORTS) { batches.add(ImportBatchEntity(id = ObjectId(), userId = TEST_USER_ID, groupId = groupId, source = null, count = 1)) }
        val token = service.read(TEST_USER_ID, group, FILE.toByteArray()).token

        assertThat(service.commit(token, TEST_USER_ID, group, emptyMap(), FREE_IMPORTS, CATEGORY_LIMIT)).isEqualTo(ImportCommit.LimitReached(FREE_IMPORTS))
    }

    private fun operation(amount: String, description: String) = FinanceOperationEntity(
        id = ObjectId(), groupId = groupId, creatorId = TEST_USER_ID, type = CategoryType.EXPENSE, amount = BigDecimal(amount),
        categoryId = checkNotNull(groceries.id), categoryName = groceries.name, operationDate = LocalDate.of(2026, 11, 3), description = description,
    )
}
