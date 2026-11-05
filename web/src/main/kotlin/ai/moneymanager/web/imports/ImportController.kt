package ai.moneymanager.web.imports

import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.imports.CategoryDecision
import ai.moneymanager.domain.model.imports.ImportPreview
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.imports.ImportCommit
import ai.moneymanager.service.imports.ImportService
import ai.moneymanager.service.imports.MAX_IMPORT_BYTES
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.ai.AiRequestGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.badRequest
import ai.moneymanager.web.error.limitReached
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private const val INVALID_FILE_KEY = "${API_ERROR_KEY_PREFIX}invalid_import_file"
private const val TOO_BIG_KEY = "${API_ERROR_KEY_PREFIX}import_too_big"
private const val EXPIRED_KEY = "${API_ERROR_KEY_PREFIX}import_expired"
private const val CATEGORY_KEY = "${API_ERROR_KEY_PREFIX}import_category"

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/imports")
class ImportController(
    private val groupAccessGuard: GroupAccessGuard,
    private val importService: ImportService,
    private val subscriptionLimitsService: SubscriptionLimitsService,
    private val aiRequestGuard: AiRequestGuard,
) {
    @PostMapping("/preview", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun preview(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody file: ByteArray): ImportPreviewResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        if (file.size > MAX_IMPORT_BYTES) throw badRequest(TOO_BIG_KEY)
        val read = runCatching { importService.read(userIdOf(user), group, file) }.getOrElse { throw badRequest(INVALID_FILE_KEY) }
        val preview = if (read.columns.none { it.role != null } && aiRequestGuard.tryQuota(user)) {
            importService.guessWithAi(read.token, userIdOf(user), group) ?: read
        } else {
            read
        }
        return respond(user, preview)
    }

    @GetMapping("/{token}")
    fun find(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable token: String): ImportPreviewResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        return respond(user, importService.find(token, userIdOf(user), group) ?: throw expired())
    }

    @PutMapping("/{token}/columns")
    fun columns(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable token: String, @RequestBody request: ImportColumnsRequest): ImportPreviewResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val preview = importService.remap(token, userIdOf(user), group, request.roles) ?: throw expired()
        return respond(user, preview)
    }

    @PostMapping("/{token}")
    @ResponseStatus(HttpStatus.CREATED)
    fun commit(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable token: String, @RequestBody request: ImportCommitRequest): ImportResultResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val paid = user.hasActivePaidSubscription()
        val decisions = request.categories.mapValues { (_, choice) -> decisionOf(choice) }
        return when (val result = importService.commit(token, userIdOf(user), group, decisions, subscriptionLimitsService.maxImports(paid), subscriptionLimitsService.maxCategoriesPerType(paid))) {
            is ImportCommit.Done -> with(result.result) {
                ImportResultResponse(importId, count, createdCategories, from, to, historyLimited = from?.let { subscriptionLimitsService.isBeyondHistoryLimit(it, paid) } ?: false)
            }
            is ImportCommit.LimitReached -> throw limitReached(ApiErrorCode.IMPORT_LIMIT_REACHED, result.limit)
            ImportCommit.Expired -> throw expired()
        }
    }

    @DeleteMapping("/batches/{importId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun undo(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable importId: ObjectId) {
        val group = groupAccessGuard.requireMember(user, groupId)
        if (!importService.undo(userIdOf(user), group, importId)) throw ApiException(ApiErrorCode.NOT_FOUND)
    }

    private fun respond(user: UserInfo, preview: ImportPreview): ImportPreviewResponse {
        val left = subscriptionLimitsService.maxImports(user.hasActivePaidSubscription())?.let { (it - importService.importsUsed(userIdOf(user))).coerceAtLeast(0) }
        return preview.toResponse(left)
    }

    private fun decisionOf(choice: CategoryChoiceRequest): CategoryDecision = when (choice.action) {
        CategoryAction.CREATE -> CategoryDecision.Create
        CategoryAction.SKIP -> CategoryDecision.Skip
        CategoryAction.EXISTING -> CategoryDecision.Existing(choice.categoryId ?: throw badRequest(CATEGORY_KEY))
    }

    private fun userIdOf(user: UserInfo): Long = checkNotNull(user.telegramUserId)

    private fun expired() = ApiException(ApiErrorCode.NOT_FOUND, EXPIRED_KEY)
}
