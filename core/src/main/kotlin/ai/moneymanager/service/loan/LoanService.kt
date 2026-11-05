package ai.moneymanager.service.loan

import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.model.loan.LoanDraft
import ai.moneymanager.domain.text.matchesEntityName
import ai.moneymanager.repository.LoanRepository
import ai.moneymanager.repository.entity.LoanEntity
import ai.moneymanager.repository.entity.LoanRepayment
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private const val POSTPONE_DAYS = 7L
private const val MAX_PEOPLE = 6

@Service
class LoanService(
    private val loanRepository: LoanRepository,
) {
    fun loans(groupId: ObjectId, creatorId: Long): List<LoanEntity> =
        loanRepository.findByGroupIdAndCreatorId(groupId, creatorId).sortedByDescending { it.lentOn }

    fun people(groupId: ObjectId, creatorId: Long): List<String> =
        loans(groupId, creatorId).map { it.person }.distinctBy { it.lowercase() }.take(MAX_PEOPLE)

    fun create(groupId: ObjectId, creatorId: Long, draft: LoanDraft): LoanEntity =
        loanRepository.save(
            LoanEntity(
                groupId = groupId,
                creatorId = creatorId,
                direction = draft.direction,
                person = draft.person.trim(),
                amount = draft.amount,
                lentOn = draft.date,
                dueDate = draft.dueDate,
                note = draft.note,
            )
        )

    fun repay(groupId: ObjectId, creatorId: Long, loanId: ObjectId, amount: BigDecimal?, date: LocalDate): LoanEntity? {
        val loan = loanRepository.findByGroupIdAndCreatorIdAndId(groupId, creatorId, loanId)?.takeIf { it.open } ?: return null
        return repay(loan, amount, date)
    }

    fun repayPerson(groupId: ObjectId, creatorId: Long, direction: LoanDirection, person: String, amount: BigDecimal?, date: LocalDate): LoanEntity? =
        openLoanOf(groupId, creatorId, direction, person)?.let { repay(it, amount, date) }

    fun openLoanOf(groupId: ObjectId, creatorId: Long, direction: LoanDirection, person: String): LoanEntity? =
        loans(groupId, creatorId)
            .filter { it.open && it.direction == direction && matchesEntityName(it.person, person) }
            .minByOrNull { it.lentOn }

    fun undoRepayment(groupId: ObjectId, creatorId: Long, loanId: ObjectId, repaymentId: ObjectId): LoanEntity? {
        val loan = loanRepository.findByGroupIdAndCreatorIdAndId(groupId, creatorId, loanId) ?: return null
        if (loan.repayments.none { it.id == repaymentId }) return null
        return loanRepository.save(loan.copy(repayments = loan.repayments.filterNot { it.id == repaymentId }))
    }

    fun postpone(loanId: ObjectId, creatorId: Long, today: LocalDate): LoanEntity? {
        val loan = loanRepository.findById(loanId).orElse(null)?.takeIf { it.creatorId == creatorId && it.open } ?: return null
        val from = listOfNotNull(loan.dueDate, today).max()
        return loanRepository.save(loan.copy(dueDate = from.plusDays(POSTPONE_DAYS)))
    }

    fun repayAll(loanId: ObjectId, creatorId: Long, date: LocalDate): LoanEntity? {
        val loan = loanRepository.findById(loanId).orElse(null)?.takeIf { it.creatorId == creatorId && it.open } ?: return null
        return repay(loan, null, date)
    }

    fun delete(groupId: ObjectId, creatorId: Long, loanId: ObjectId): Boolean {
        val loan = loanRepository.findByGroupIdAndCreatorIdAndId(groupId, creatorId, loanId) ?: return false
        loanRepository.delete(loan)
        return true
    }

    fun dueBy(date: LocalDate): List<LoanEntity> =
        loanRepository.findByDueDateLessThanEqual(date).filter { it.open && it.dueRemindedOn != it.dueDate }

    fun claimDueReminder(loan: LoanEntity): Boolean {
        if (loan.dueRemindedOn == loan.dueDate) return false
        loanRepository.save(loan.copy(dueRemindedOn = loan.dueDate))
        return true
    }

    private fun repay(loan: LoanEntity, amount: BigDecimal?, date: LocalDate): LoanEntity {
        val paid = (amount ?: loan.remaining).min(loan.remaining)
        require(paid.signum() > 0) { "A repayment moves a positive amount" }
        return loanRepository.save(loan.copy(repayments = loan.repayments + LoanRepayment(amount = paid, date = date)))
    }
}
