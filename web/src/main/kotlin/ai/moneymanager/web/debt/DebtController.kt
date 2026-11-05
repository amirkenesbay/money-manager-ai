package ai.moneymanager.web.debt

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.debt.GroupDebts
import ai.moneymanager.domain.model.debt.Settlement
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.debt.DebtService
import ai.moneymanager.service.debt.ReminderResult
import ai.moneymanager.service.loan.LoanService
import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.repository.entity.LoanEntity
import ai.moneymanager.web.loan.toResponse
import java.math.BigDecimal
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.badRequest
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

private const val INVALID_AMOUNT_KEY = "${API_ERROR_KEY_PREFIX}invalid_amount"
private const val INVALID_SHARE_KEY = "${API_ERROR_KEY_PREFIX}invalid_share"
private const val NO_DEBT_KEY = "${API_ERROR_KEY_PREFIX}no_debt"

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/debts")
class DebtController(
    private val groupAccessGuard: GroupAccessGuard,
    private val debtService: DebtService,
    private val userInfoService: UserInfoService,
    private val userCalendar: UserCalendar,
    private val loanService: LoanService,
) {
    @GetMapping
    fun debts(user: UserInfo, @PathVariable groupId: ObjectId): DebtsResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val userId = checkNotNull(user.telegramUserId)
        return debtService.debts(group, userId).toResponse(userId, loanService.loans(groupId, userId))
    }

    @PostMapping("/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    fun settle(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: SettlementRequest): SettlementResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        if (request.amount.signum() <= 0) throw badRequest(INVALID_AMOUNT_KEY)
        val userId = checkNotNull(user.telegramUserId)
        val settlement = debtService.settle(group, userId, request.fromId, request.toId, request.amount, userCalendar.today(user))
            ?: throw badRequest(INVALID_SHARE_KEY)
        return settlement.toResponse(userId, userInfoService.displayNames(setOf(settlement.fromId, settlement.toId)))
    }

    @DeleteMapping("/settlements/{settlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun undo(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable settlementId: ObjectId) {
        val group = groupAccessGuard.requireMember(user, groupId)
        if (!debtService.undo(group, checkNotNull(user.telegramUserId), settlementId)) throw ApiException(ApiErrorCode.NOT_FOUND)
    }

    @PostMapping("/reminders")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remind(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: ReminderRequest) {
        val group = groupAccessGuard.requireMember(user, groupId)
        when (debtService.remind(group, checkNotNull(user.telegramUserId), request.userId)) {
            ReminderResult.SENT -> Unit
            ReminderResult.TOO_SOON -> throw ApiException(ApiErrorCode.REMINDER_TOO_SOON)
            ReminderResult.NO_DEBT -> throw badRequest(NO_DEBT_KEY)
        }
    }

    private fun GroupDebts.toResponse(userId: Long, loans: List<LoanEntity>): DebtsResponse {
        val names = userInfoService.displayNames(mine.map { it.userId }.toSet() + others.flatMap { listOf(it.fromId, it.toId) } + history.flatMap { listOf(it.fromId, it.toId) })
        val open = loans.filter { it.open }
        val owedToMe = sumOf(mine.map { it.amount }.filter { it.signum() > 0 } + open.filter { it.direction == LoanDirection.LENT }.map { it.remaining })
        val iOwe = sumOf(mine.map { it.amount.negate() }.filter { it.signum() > 0 } + open.filter { it.direction == LoanDirection.BORROWED }.map { it.remaining })
        return DebtsResponse(
            balance = owedToMe - iOwe,
            owedToMe = owedToMe,
            iOwe = iOwe,
            mine = mine.map { debt ->
                PersonalDebtResponse(debt.userId, names.getValue(debt.userId), debt.amount, debt.items.map {
                    DebtItemResponse(it.kind, it.amount, it.date, it.operationId, it.icon, it.title)
                })
            },
            others = others.map { TransferResponse(it.fromId, names.getValue(it.fromId), it.toId, names.getValue(it.toId), it.amount) },
            history = history.map { it.toResponse(userId, names) },
            loans = loans.map { it.toResponse() },
        )
    }

    private fun sumOf(amounts: List<BigDecimal>): BigDecimal = amounts.fold(BigDecimal.ZERO, BigDecimal::add)

    private fun Settlement.toResponse(userId: Long, names: Map<Long, String>) =
        SettlementResponse(id, fromId, names.getValue(fromId), toId, names.getValue(toId), amount, date, canUndo = creatorId == userId)
}
