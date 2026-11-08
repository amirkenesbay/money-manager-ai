package ai.moneymanager.service

import ai.moneymanager.domain.model.CURRENT_DEFAULTS_VERSION
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DefaultCategory
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.entity.CategoryEntity
import ai.moneymanager.repository.entity.MoneyGroupEntity
import ai.moneymanager.repository.entity.common.AuditInfo
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

private const val OWNER_ID = 7L
private val START = LocalDateTime.of(2026, 1, 1, 0, 0)

class DefaultCategoriesMigrationTest {
    private val groupId = ObjectId()
    private val group = MoneyGroupEntity(groupId, "Личное", "token", OWNER_ID, setOf(OWNER_ID), GroupType.PERSONAL, defaultCategoriesVersion = 1)
    private val categoryRepository = mock(CategoryRepository::class.java)
    private val groupRepository = mock(MoneyGroupRepository::class.java)
    private val localization = testLocalizationService()
    private val migration = DefaultCategoriesMigration(
        groupRepository,
        categoryRepository,
        CategoryService(categoryRepository, localization) {},
        mock(UserInfoService::class.java),
        localization,
    )

    private fun old(name: String, icon: String, type: CategoryType, order: Long) =
        CategoryEntity(ObjectId(), name, icon, type, groupId, auditInfo = AuditInfo(createdAt = START.plusMinutes(order)))

    private val oldCategories = listOf(
        old("Продукты", "🛒", CategoryType.EXPENSE, 0),
        old("Транспорт", "🚗", CategoryType.EXPENSE, 1),
        old("Развлечения", "🎬", CategoryType.EXPENSE, 2),
        old("Здоровье", "💊", CategoryType.EXPENSE, 3),
        old("Образование", "📚", CategoryType.EXPENSE, 4),
        old("Другое", "📦", CategoryType.EXPENSE, 5),
        old("Зарплата", "💰", CategoryType.INCOME, 6),
        old("Подарки", "🎁", CategoryType.INCOME, 7),
        old("Другое", "📦", CategoryType.INCOME, 8),
        old("Такси", "🚕", CategoryType.EXPENSE, 9),
    )

    private fun migrate(): List<CategoryEntity> {
        val saved = mutableListOf<CategoryEntity>()
        `when`(categoryRepository.findByGroupId(groupId)).thenReturn(oldCategories)
        `when`(categoryRepository.saveAll(anyList<CategoryEntity>())).thenAnswer { it.getArgument<List<CategoryEntity>>(0).also(saved::addAll) }
        `when`(groupRepository.save(any(MoneyGroupEntity::class.java))).thenAnswer { it.getArgument(0) }
        migration.migrate(group)
        return saved
    }

    @Test
    fun `adds only the new defaults and keeps a deleted old default deleted`() {
        val names = migrate().map { it.name }

        assertThat(names).contains("Кафе и рестораны", "Дом и коммуналка", "Связь и интернет", "Подписки", "Красота", "Подработка", "Кэшбэк", "Инвестиции")
        assertThat(names).doesNotContain("Одежда")
        assertThat(names).hasSize(oldCategories.size + DefaultCategory.entries.count { it.since == CURRENT_DEFAULTS_VERSION })
    }

    @Test
    fun `keeps Other last among expenses and numbers every category in order`() {
        val saved = migrate()
        val expenses = saved.filter { it.type == CategoryType.EXPENSE }

        assertThat(saved.map { it.position }).isEqualTo(saved.indices.toList())
        assertThat(expenses.indexOfFirst { it.name == "Другое" }).isGreaterThan(expenses.indexOfFirst { it.name == "Красота" })
    }

    @Test
    fun `marks old defaults so they no longer count against the limit and leaves own categories unmarked`() {
        val saved = migrate()

        assertThat(saved.first { it.name == "Продукты" }.seedKey).isEqualTo(DefaultCategory.GROCERIES.name)
        assertThat(saved.first { it.name == "Другое" && it.type == CategoryType.INCOME }.seedKey).isEqualTo(DefaultCategory.OTHER_INCOME.name)
        assertThat(saved.first { it.name == "Такси" }.seedKey).isNull()
    }
}
