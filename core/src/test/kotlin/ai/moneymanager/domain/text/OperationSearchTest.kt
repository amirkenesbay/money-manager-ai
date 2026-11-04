package ai.moneymanager.domain.text

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class OperationSearchTest {
    private val coffee = operation("1500", "Кафе", "Капучино #Отпуск #отпуск #море")

    @Test
    fun `reads tags once in lowercase`() {
        assertThat(tagsOf(coffee.description)).containsExactly("отпуск", "море")
        assertThat(tagsOf(null)).isEmpty()
    }

    @Test
    fun `finds by description, category, exact amount or tag`() {
        assertThat(matchesSearch(coffee, "капуч")).isTrue()
        assertThat(matchesSearch(coffee, "кафе")).isTrue()
        assertThat(matchesSearch(coffee, "1 500")).isTrue()
        assertThat(matchesSearch(coffee, "1500,00")).isTrue()
        assertThat(matchesSearch(coffee, "#МОРЕ")).isTrue()
    }

    @Test
    fun `does not confuse a part of a tag or another amount`() {
        assertThat(matchesSearch(coffee, "#мор")).isFalse()
        assertThat(matchesSearch(coffee, "150")).isFalse()
        assertThat(matchesSearch(coffee, "#ремонт")).isFalse()
    }

    private fun operation(amount: String, category: String, description: String?) = FinanceOperationEntity(
        id = ObjectId(),
        groupId = ObjectId(),
        creatorId = 1L,
        type = CategoryType.EXPENSE,
        amount = BigDecimal(amount),
        categoryId = ObjectId(),
        categoryName = category,
        categoryIcon = null,
        operationDate = LocalDate.of(2026, 11, 4),
        description = description,
    )
}
