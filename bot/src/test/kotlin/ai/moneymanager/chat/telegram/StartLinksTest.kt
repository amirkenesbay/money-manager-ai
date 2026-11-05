package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.model.DigestKind
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StartLinksTest {
    @Test
    fun `reads the digest to mute from the start command of its button`() {
        DigestKind.entries.forEach { kind ->
            assertThat(mutedDigestKind("/start ${digestMuteParameter(kind)}")).isEqualTo(kind)
        }
    }

    @Test
    fun `ignores other start commands`() {
        assertThat(mutedDigestKind("/start finance")).isNull()
        assertThat(mutedDigestKind("/start")).isNull()
        assertThat(mutedDigestKind(null)).isNull()
    }

    @Test
    fun `reads the operation to undo and the recurring payment to record`() {
        val operationId = ObjectId()
        val paymentId = ObjectId()
        val date = LocalDate.of(2026, 11, 15)

        assertThat(undoOperationId("/start ${undoOperationParameter(operationId)}")).isEqualTo(operationId)
        assertThat(recurringRecordRequest("/start ${recordRecurringParameter(paymentId, date)}")).isEqualTo(RecurringRecordRequest(paymentId, date))
    }

    @Test
    fun `ignores broken links`() {
        assertThat(undoOperationId("/start undo_nope")).isNull()
        assertThat(recurringRecordRequest("/start pay_${ObjectId()}_yesterday")).isNull()
        assertThat(recurringRecordRequest("/start pay_")).isNull()
    }

    @Test
    fun `reads the debt to mark as paid back`() {
        val groupId = ObjectId()

        assertThat(debtPaidRequest("/start ${debtPaidParameter(groupId, 42L)}")).isEqualTo(DebtPaidRequest(groupId, 42L))
        assertThat(debtPaidRequest("/start debt_${groupId}_someone")).isNull()
        assertThat(debtPaidRequest("/start debt_nope_42")).isNull()
    }

    @Test
    fun `reads the loan to close or postpone from a due reminder`() {
        val loanId = ObjectId()

        assertThat(repaidLoanId("/start ${loanRepaidParameter(loanId)}")).isEqualTo(loanId)
        assertThat(postponedLoanId("/start ${loanLaterParameter(loanId)}")).isEqualTo(loanId)
        assertThat(repaidLoanId("/start ${loanLaterParameter(loanId)}")).isNull()
    }
}
