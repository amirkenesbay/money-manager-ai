package ai.moneymanager.service

import ai.moneymanager.domain.event.CategoryCorrected
import ai.moneymanager.domain.event.CategoryDeleted
import ai.moneymanager.repository.CategoryRuleRepository
import ai.moneymanager.repository.entity.CategoryRuleEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.any

class CategoryRuleServiceTest {
    private val groupId = ObjectId()
    private val groceries = ObjectId()
    private val repository = mock(CategoryRuleRepository::class.java)
    private val service = CategoryRuleService(repository)

    @Test
    fun `remembers the category of a corrected place`() {
        service.onCategoryCorrected(CategoryCorrected(groupId, "Магнум!", groceries))

        assertThat(savedRule()).isEqualTo(CategoryRuleEntity(groupId = groupId, merchantKey = "магнум", categoryId = groceries))
    }

    @Test
    fun `moves an already remembered place to the new category`() {
        val existing = CategoryRuleEntity(ObjectId(), groupId, "магнум", ObjectId())
        `when`(repository.findByGroupIdAndMerchantKey(groupId, "магнум")).thenReturn(existing)

        service.onCategoryCorrected(CategoryCorrected(groupId, "магнум", groceries))

        assertThat(savedRule()).isEqualTo(existing.copy(categoryId = groceries))
    }

    @Test
    fun `remembers nothing without a description`() {
        service.onCategoryCorrected(CategoryCorrected(groupId, null, groceries))

        verify(repository, never()).save(any(CategoryRuleEntity::class.java))
    }

    @Test
    fun `forgets the places of a deleted category`() {
        service.onCategoryDeleted(CategoryDeleted(groupId, groceries, 1L, "🛒", "Продукты"))

        verify(repository).deleteByCategoryId(groceries)
    }

    @Test
    fun `does not forget a place of another group`() {
        assertThat(service.forget(groupId, ObjectId())).isFalse()
    }

    private fun savedRule(): CategoryRuleEntity {
        val captor = ArgumentCaptor.forClass(CategoryRuleEntity::class.java)
        verify(repository).save(captor.capture())
        return captor.value
    }
}
