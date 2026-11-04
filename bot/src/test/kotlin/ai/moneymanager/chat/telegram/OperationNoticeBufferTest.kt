package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class OperationNoticeBufferTest {
    private val original = operation("3500")
    private val edited = original.copy(amount = BigDecimal("4000"))

    @Test
    fun `drops an operation that was added and deleted in the same batch`() {
        assertThat(collapse(listOf(OperationChange.Added(original), OperationChange.Deleted(edited)))).isEmpty()
    }

    @Test
    fun `shows an added then edited operation as added with its final values`() {
        assertThat(collapse(listOf(OperationChange.Added(original), OperationChange.Edited(original, edited))))
            .containsExactly(OperationChange.Added(edited))
    }

    @Test
    fun `merges several edits into one from the first state to the last`() {
        val again = edited.copy(amount = BigDecimal("4500"))

        assertThat(collapse(listOf(OperationChange.Edited(original, edited), OperationChange.Edited(edited, again))))
            .containsExactly(OperationChange.Edited(original, again))
    }

    @Test
    fun `forgets edits that were undone`() {
        assertThat(collapse(listOf(OperationChange.Edited(original, edited), OperationChange.Edited(edited, original)))).isEmpty()
    }

    @Test
    fun `reports a deleted operation as it was before the batch`() {
        assertThat(collapse(listOf(OperationChange.Edited(original, edited), OperationChange.Deleted(edited))))
            .containsExactly(OperationChange.Deleted(original))
    }

    private fun operation(amount: String) = FinanceOperationEntity(
        id = ObjectId(),
        groupId = ObjectId(),
        creatorId = 1L,
        type = CategoryType.EXPENSE,
        amount = BigDecimal(amount),
        categoryId = ObjectId(),
        categoryName = "Продукты",
        categoryIcon = "🛒",
        operationDate = LocalDate.of(2026, 11, 4),
        description = null,
    )
}
