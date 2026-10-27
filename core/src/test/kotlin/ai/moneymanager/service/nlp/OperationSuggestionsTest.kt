package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.OperationSuggestion
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 10, 28)

class OperationSuggestionsTest {
    private val groupId = ObjectId()
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId)
    private val salary = Category(ObjectId(), "Зарплата", "💰", CategoryType.INCOME, groupId)
    private val categories = listOf(taxi, salary)

    @Test
    fun `matches an existing category of the same type`() {
        val suggestion = suggestOperation(expense(category = "такси", date = "2026-10-27"), categories, TODAY)

        assertThat(suggestion).isEqualTo(OperationSuggestion.Ready(CategoryType.EXPENSE, 2000.0, taxi, null, TODAY.minusDays(1)))
    }

    @Test
    fun `offers a new category with the suggested icon`() {
        val suggestion = suggestOperation(expense(category = "Кафе", icon = "☕"), categories, TODAY)

        assertThat(suggestion).isEqualTo(OperationSuggestion.NewCategory(CategoryType.EXPENSE, 2000.0, "Кафе", "☕", null, TODAY))
    }

    @Test
    fun `keeps the amount when the category is missing and moves a future date to today`() {
        val suggestion = suggestOperation(expense(category = null, date = "2026-11-02"), categories, TODAY)

        assertThat(suggestion).isEqualTo(OperationSuggestion.CategoryMissing(CategoryType.EXPENSE, 2000.0, null, TODAY))
    }

    @Test
    fun `rejects a non positive amount and ignores other commands`() {
        assertThat(suggestOperation(expense(amount = 0.0), categories, TODAY)).isEqualTo(OperationSuggestion.AmountNotPositive)
        assertThat(suggestOperation(BotCommand.ShowBalance, categories, TODAY)).isNull()
    }

    private fun expense(amount: Double = 2000.0, category: String? = "Такси", icon: String? = null, date: String? = null) =
        BotCommand.AddExpense(amount, category, null, icon, date)
}
