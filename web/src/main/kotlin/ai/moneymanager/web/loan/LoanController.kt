package ai.moneymanager.web.loan

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.loan.LoanDraft
import ai.moneymanager.service.loan.LoanService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.badRequest
import ai.moneymanager.web.finance.MAX_DESCRIPTION_LENGTH
import ai.moneymanager.web.time.UserCalendar
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

const val MAX_PERSON_LENGTH = 50

private const val INVALID_AMOUNT_KEY = "${API_ERROR_KEY_PREFIX}invalid_amount"
private const val INVALID_PERSON_KEY = "${API_ERROR_KEY_PREFIX}invalid_person"
private const val FUTURE_DATE_KEY = "${API_ERROR_KEY_PREFIX}future_date"
private const val DESCRIPTION_TOO_LONG_KEY = "${API_ERROR_KEY_PREFIX}description_too_long"

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/loans")
class LoanController(
    private val groupAccessGuard: GroupAccessGuard,
    private val loanService: LoanService,
    private val userCalendar: UserCalendar,
) {
    @GetMapping("/people")
    fun people(user: UserInfo, @PathVariable groupId: ObjectId): List<String> {
        groupAccessGuard.requireMember(user, groupId)
        return loanService.people(groupId, checkNotNull(user.telegramUserId))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: LoanRequest): LoanResponse {
        groupAccessGuard.requireMember(user, groupId)
        return loanService.create(groupId, checkNotNull(user.telegramUserId), draftOf(user, request)).toResponse()
    }

    @PostMapping("/{loanId}/repayments")
    @ResponseStatus(HttpStatus.CREATED)
    fun repay(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable loanId: ObjectId, @RequestBody request: RepaymentRequest): LoanResponse {
        groupAccessGuard.requireMember(user, groupId)
        if (request.amount != null && request.amount.signum() <= 0) throw badRequest(INVALID_AMOUNT_KEY)
        return loanService.repay(groupId, checkNotNull(user.telegramUserId), loanId, request.amount, userCalendar.today(user))?.toResponse() ?: throw notFound()
    }

    @DeleteMapping("/{loanId}/repayments/{repaymentId}")
    fun undoRepayment(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable loanId: ObjectId, @PathVariable repaymentId: ObjectId): LoanResponse {
        groupAccessGuard.requireMember(user, groupId)
        return loanService.undoRepayment(groupId, checkNotNull(user.telegramUserId), loanId, repaymentId)?.toResponse() ?: throw notFound()
    }

    @DeleteMapping("/{loanId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable loanId: ObjectId) {
        groupAccessGuard.requireMember(user, groupId)
        if (!loanService.delete(groupId, checkNotNull(user.telegramUserId), loanId)) throw notFound()
    }

    private fun draftOf(user: UserInfo, request: LoanRequest): LoanDraft {
        if (request.amount.signum() <= 0) throw badRequest(INVALID_AMOUNT_KEY)
        val person = request.person?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_PERSON_LENGTH } ?: throw badRequest(INVALID_PERSON_KEY, MAX_PERSON_LENGTH)
        val today = userCalendar.today(user)
        val date = request.date ?: today
        if (date.isAfter(today)) throw badRequest(FUTURE_DATE_KEY)
        val note = request.note?.trim()?.takeIf { it.isNotEmpty() }
        if (note != null && note.length > MAX_DESCRIPTION_LENGTH) throw badRequest(DESCRIPTION_TOO_LONG_KEY, MAX_DESCRIPTION_LENGTH)
        return LoanDraft(request.direction, person, request.amount, date, request.dueDate?.takeIf { it.isAfter(date) }, note)
    }

    private fun notFound() = ApiException(ApiErrorCode.NOT_FOUND)
}
