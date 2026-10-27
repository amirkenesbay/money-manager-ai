package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.OperationDraft
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.badRequest
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

const val MAX_DESCRIPTION_LENGTH = 200

private const val INVALID_AMOUNT_KEY = "${API_ERROR_KEY_PREFIX}invalid_amount"
private const val FUTURE_DATE_KEY = "${API_ERROR_KEY_PREFIX}future_date"
private const val DESCRIPTION_TOO_LONG_KEY = "${API_ERROR_KEY_PREFIX}description_too_long"
private const val CATEGORY_TYPE_MISMATCH_KEY = "${API_ERROR_KEY_PREFIX}category_type_mismatch"
private val EARLIEST_ZONE_ON_EARTH: ZoneId = ZoneOffset.MAX

@Component
class OperationDraftFactory(
    private val groupCategoryLookup: GroupCategoryLookup,
    private val clock: Clock,
) {
    fun create(user: UserInfo, groupId: ObjectId, request: OperationRequest): OperationDraft {
        if (request.amount.signum() <= 0) throw badRequest(INVALID_AMOUNT_KEY)
        if (request.operationDate.isAfter(today(user))) throw badRequest(FUTURE_DATE_KEY)
        val description = request.description?.trim()?.takeIf { it.isNotEmpty() }
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            throw badRequest(DESCRIPTION_TOO_LONG_KEY, MAX_DESCRIPTION_LENGTH)
        }
        val category = groupCategoryLookup.requireCategory(groupId, request.categoryId)
        if (category.type != request.type) throw badRequest(CATEGORY_TYPE_MISMATCH_KEY)
        return OperationDraft(request.type, request.amount, category, request.operationDate, description)
    }

    private fun today(user: UserInfo): LocalDate = LocalDate.now(clock.withZone(userZone(user)))

    private fun userZone(user: UserInfo): ZoneId =
        user.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: EARLIEST_ZONE_ON_EARTH
}
