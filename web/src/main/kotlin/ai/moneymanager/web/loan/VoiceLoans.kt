package ai.moneymanager.web.loan

import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.loan.LoanService
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

enum class VoiceLoanKind {
    NEW,
    REPAYMENT,
}

data class VoiceLoanResponse(
    val kind: VoiceLoanKind,
    val direction: LoanDirection,
    val person: String,
    val amount: BigDecimal?,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val loanId: ObjectId? = null,
    val remaining: BigDecimal? = null,
)

@Component
class VoiceLoans(
    private val loanService: LoanService,
) {
    fun loanOf(groupId: ObjectId, userId: Long, commands: List<BotCommand>, today: LocalDate): VoiceLoanResponse? =
        commands.firstNotNullOfOrNull { command ->
            when (command) {
                is BotCommand.AddLoan -> command.draft(today).let {
                    VoiceLoanResponse(VoiceLoanKind.NEW, it.direction, it.person, it.amount, it.dueDate, it.note)
                }
                is BotCommand.RepayLoan -> {
                    val open = loanService.openLoanOf(groupId, userId, command.direction, command.person)
                    val asked = command.amount?.let { BigDecimal.valueOf(it) }
                    VoiceLoanResponse(VoiceLoanKind.REPAYMENT, command.direction, open?.person ?: command.person, asked ?: open?.remaining, loanId = open?.id, remaining = open?.remaining)
                }
                else -> null
            }
        }
}
