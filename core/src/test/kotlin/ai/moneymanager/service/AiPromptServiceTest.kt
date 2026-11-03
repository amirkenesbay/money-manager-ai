package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryRule
import ai.moneymanager.domain.model.CategoryType
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test

class AiPromptServiceTest {
    private val groupId = ObjectId()
    private val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
    private val service = AiPromptService()

    @Test
    fun `tells the AI which category the user picked for a place before`() {
        val rules = listOf(CategoryRule(ObjectId(), "магнум", checkNotNull(groceries.id)))

        assertThat(service.rememberedPlacesPreamble(listOf(groceries), rules)).endsWith("магнум → Продукты")
    }

    @Test
    fun `adds nothing when no place is remembered or its category is gone`() {
        val orphan = listOf(CategoryRule(ObjectId(), "кофе", ObjectId()))

        assertThat(service.rememberedPlacesPreamble(listOf(groceries), emptyList())).isEmpty()
        assertThat(service.rememberedPlacesPreamble(listOf(groceries), orphan)).isEmpty()
    }
}
