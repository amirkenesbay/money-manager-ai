package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryRule
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Recurrence
import ai.moneymanager.domain.model.RecurrenceUnit
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

    @Test
    fun `uses the remembered category of a place over the one the AI picked`() {
        val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
        val rules = listOf(CategoryRule(ObjectId(), "магнум", checkNotNull(groceries.id)))

        val suggestion = suggestOperation(expense(category = "Кафе", description = "МАГНУМ!"), categories + groceries, TODAY, rules)

        assertThat(suggestion).isEqualTo(OperationSuggestion.Ready(CategoryType.EXPENSE, 2000.0, groceries, "МАГНУМ!", TODAY))
    }

    @Test
    fun `ignores a remembered category of the other operation type`() {
        val rules = listOf(CategoryRule(ObjectId(), "магнум", checkNotNull(salary.id)))

        val suggestion = suggestOperation(expense(category = "такси", description = "Магнум"), categories, TODAY, rules)

        assertThat((suggestion as OperationSuggestion.Ready).category).isEqualTo(taxi)
    }

    @Test
    fun `reads how often a payment repeats`() {
        assertThat(repeatOf(BotCommand.AddExpense(3500.0, "Подписки", "Netflix", null, null, "month", null))).isEqualTo(Recurrence(RecurrenceUnit.MONTH))
        assertThat(repeatOf(BotCommand.AddExpense(9000.0, "Связь", null, null, null, "MONTH", 3.0))).isEqualTo(Recurrence(RecurrenceUnit.MONTH, 3))
        assertThat(repeatOf(expense())).isNull()
        assertThat(repeatOf(BotCommand.AddExpense(1.0, null, null, null, null, "fortnight", null))).isNull()
    }

    private fun expense(amount: Double = 2000.0, category: String? = "Такси", icon: String? = null, date: String? = null, description: String? = null) =
        BotCommand.AddExpense(amount, category, description, icon, date)
}
