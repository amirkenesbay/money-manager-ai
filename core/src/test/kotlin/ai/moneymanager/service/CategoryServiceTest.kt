package ai.moneymanager.service

import ai.moneymanager.domain.event.CategoryDeleted
import ai.moneymanager.domain.event.CategoryRenamed
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

private const val ACTOR_ID = 42L

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
        assertThat(publishedEvents).containsExactly(CategoryRenamed(groupId, ACTOR_ID, "🚕", "Такси", "Такси и каршеринг"))
    }

    @Test
    fun `refuses a name another category of the same type already has`() {
        val result = service.updateCategory(ACTOR_ID, taxi.id!!, "Транспорт", "🚕")

        assertThat(result).isEqualTo(CategoryUpdateResult.Duplicate)
        assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `changes only the icon without a group notice`() {
        val result = service.updateCategory(ACTOR_ID, taxi.id!!, "Такси", "🚖")

        assertThat((result as CategoryUpdateResult.Updated).category.icon).isEqualTo("🚖")
        assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `deletes a category and tells the group who did it`() {
        assertThat(service.deleteCategory(ACTOR_ID, taxi.id!!)).isTrue()

        assertThat(publishedEvents).containsExactly(CategoryDeleted(groupId, ACTOR_ID, "🚕", "Такси"))
    }

    @Test
    fun `reports a missing category on delete`() {
        `when`(repository.findById(transport.id!!)).thenReturn(Optional.empty())

        assertThat(service.deleteCategory(ACTOR_ID, transport.id!!)).isFalse()
        assertThat(publishedEvents).isEmpty()
    }
}
