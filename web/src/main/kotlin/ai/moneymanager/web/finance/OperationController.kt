package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

private const val OPERATION_NOT_FOUND_KEY = "${API_ERROR_KEY_PREFIX}operation_not_found"

data class OperationRequest(
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryId: ObjectId,
    val operationDate: LocalDate,
    val description: String?,
    val categoryCorrected: Boolean = false,
    val payerId: Long? = null,
    val participantIds: Set<Long>? = null,
)

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/operations")
class OperationController(
    private val groupAccessGuard: GroupAccessGuard,
    private val financeOperationService: FinanceOperationService,
    private val operationDraftFactory: OperationDraftFactory,
    private val financeResponseFactory: FinanceResponseFactory,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: OperationRequest): OperationResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val draft = operationDraftFactory.create(user, group, request)
        return financeResponseFactory.operation(financeOperationService.create(groupId, checkNotNull(user.telegramUserId), draft, request.categoryCorrected))
    }

    @PutMapping("/{operationId}")
    fun replace(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @PathVariable operationId: ObjectId,
        @RequestBody request: OperationRequest,
    ): OperationResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val draft = operationDraftFactory.create(user, group, request)
        val updated = financeOperationService.replace(checkNotNull(user.telegramUserId), groupId, operationId, draft) ?: throw operationNotFound()
        return financeResponseFactory.operation(updated)
    }

    @DeleteMapping("/{operationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable operationId: ObjectId) {
        groupAccessGuard.requireMember(user, groupId)
        if (!financeOperationService.deleteOperation(checkNotNull(user.telegramUserId), groupId, operationId)) throw operationNotFound()
    }

    private fun operationNotFound() = ApiException(ApiErrorCode.NOT_FOUND, OPERATION_NOT_FOUND_KEY)
}
