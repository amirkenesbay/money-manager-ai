package ai.moneymanager.web.access

import ai.moneymanager.domain.model.Category
import ai.moneymanager.service.CategoryService
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

private const val CATEGORY_NOT_FOUND_KEY = "${API_ERROR_KEY_PREFIX}category_not_found"

@Component
class GroupCategoryLookup(
    private val categoryService: CategoryService,
) {
    fun requireCategory(groupId: ObjectId, categoryId: ObjectId): Category =
        categoryService.getCategory(categoryId)?.takeIf { it.groupId == groupId }
            ?: throw ApiException(ApiErrorCode.NOT_FOUND, CATEGORY_NOT_FOUND_KEY)
}
