package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.OperationDraft
import ai.moneymanager.domain.model.Recurrence
import ai.moneymanager.domain.model.RecurringDraft
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.badRequest
import ai.moneymanager.web.time.UserCalendar
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.math.BigDecimal

const val MAX_DESCRIPTION_LENGTH = 200

private const val INVALID_AMOUNT_KEY = "${API_ERROR_KEY_PREFIX}invalid_amount"
private const val FUTURE_DATE_KEY = "${API_ERROR_KEY_PREFIX}future_date"
private const val DESCRIPTION_TOO_LONG_KEY = "${API_ERROR_KEY_PREFIX}description_too_long"
private const val CATEGORY_TYPE_MISMATCH_KEY = "${API_ERROR_KEY_PREFIX}category_type_mismatch"
private const val INVALID_INTERVAL_KEY = "${API_ERROR_KEY_PREFIX}invalid_interval"
private const val END_BEFORE_START_KEY = "${API_ERROR_KEY_PREFIX}end_before_start"

@Component
class OperationDraftFactory(
    private val groupCategoryLookup: GroupCategoryLookup,
    private val userCalendar: UserCalendar,
) {
    fun create(user: UserInfo, groupId: ObjectId, request: OperationRequest): OperationDraft {
        requirePositive(request.amount)
        if (request.operationDate.isAfter(userCalendar.today(user))) throw badRequest(FUTURE_DATE_KEY)
        val category = requireCategory(groupId, request.categoryId, request.type)
        return OperationDraft(request.type, request.amount, category, request.operationDate, descriptionOf(request.description))
    }

    fun recurring(groupId: ObjectId, request: RecurringRequest): RecurringDraft {
        requirePositive(request.amount)
        val recurrence = runCatching { Recurrence(request.unit, request.interval) }.getOrElse { throw badRequest(INVALID_INTERVAL_KEY) }
        if (request.endDate != null && request.endDate.isBefore(request.startDate)) throw badRequest(END_BEFORE_START_KEY)
        val category = requireCategory(groupId, request.categoryId, request.type)
        return RecurringDraft(request.type, request.amount, category, descriptionOf(request.description), recurrence, request.startDate, request.endDate, request.mode)
    }

    private fun requirePositive(amount: BigDecimal) {
        if (amount.signum() <= 0) throw badRequest(INVALID_AMOUNT_KEY)
    }

    private fun descriptionOf(raw: String?): String? {
        val description = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (description.length > MAX_DESCRIPTION_LENGTH) throw badRequest(DESCRIPTION_TOO_LONG_KEY, MAX_DESCRIPTION_LENGTH)
        return description
    }

    private fun requireCategory(groupId: ObjectId, categoryId: ObjectId, type: CategoryType): Category {
        val category = groupCategoryLookup.requireCategory(groupId, categoryId)
        if (category.type != type) throw badRequest(CATEGORY_TYPE_MISMATCH_KEY)
        return category
    }
}
