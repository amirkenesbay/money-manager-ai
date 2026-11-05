package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.ExpenseShare
import ai.moneymanager.domain.model.nlp.BotCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val ME = 1L
private const val ANYA = 2L
private const val DANYA = 3L
private val MEMBERS = mapOf(ME to "Амир", ANYA to "Аня", DANYA to "Даня")

class ExpenseSharesTest {
    @Test
    fun `reads who paid and who it was for`() {
        assertThat(shareOf(paidBy = "Даня", splitWith = "ME,Даня")).isEqualTo(ExpenseShare(DANYA, setOf(ME, DANYA)))
        assertThat(shareOf(paidBy = "аня", splitWith = "ALL")).isEqualTo(ExpenseShare(ANYA, setOf(ME, ANYA, DANYA)))
        assertThat(shareOf(paidBy = null, splitWith = "Аня")).isEqualTo(ExpenseShare(ME, setOf(ANYA)))
    }

    @Test
    fun `leaves the default when nothing was said or a name is unknown`() {
        assertThat(shareOf(paidBy = null, splitWith = null)).isNull()
        assertThat(shareOf(paidBy = "Серик", splitWith = "ALL")).isNull()
        assertThat(shareOf(paidBy = "Даня", splitWith = "ME,Серик")).isNull()
    }

    private fun shareOf(paidBy: String?, splitWith: String?) =
        expenseShareOf(BotCommand.AddExpense(3000.0, "Такси", null, null, null, paidBy = paidBy, splitWith = splitWith), ME, MEMBERS)
}
