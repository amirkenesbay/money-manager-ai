package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.nlp.arguments.AddLoanArgs
import ai.moneymanager.domain.model.nlp.arguments.RepayLoanArgs
import ai.moneymanager.domain.text.geminiValue
import ai.moneymanager.domain.text.parseIsoDateOrNull
import java.math.BigDecimal

fun addLoanOf(args: AddLoanArgs): BotCommand.AddLoan? {
    val direction = directionOf(args.direction) ?: return null
    val person = args.person.geminiValue() ?: return null
    if (args.amount <= 0) return null
    return BotCommand.AddLoan(direction, person, BigDecimal.valueOf(args.amount), parseIsoDateOrNull(args.dueDate.geminiValue()), args.note.geminiValue())
}

fun repayLoanOf(args: RepayLoanArgs): BotCommand.RepayLoan? {
    val direction = directionOf(args.direction) ?: return null
    val person = args.person.geminiValue() ?: return null
    return BotCommand.RepayLoan(direction, person, args.amount?.takeIf { it > 0 })
}

private fun directionOf(name: String?): LoanDirection? = LoanDirection.entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) }
