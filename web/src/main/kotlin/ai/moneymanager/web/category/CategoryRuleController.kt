package ai.moneymanager.web.category

import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.CategoryRuleService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private const val CATEGORY_RULE_NOT_FOUND_KEY = "${API_ERROR_KEY_PREFIX}category_rule_not_found"

data class CategoryRuleResponse(
    val id: ObjectId,
    val place: String,
    val categoryId: ObjectId,
    val categoryName: String,
    val categoryIcon: String,
)

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/category-rules")
class CategoryRuleController(
    private val groupAccessGuard: GroupAccessGuard,
    private val categoryRuleService: CategoryRuleService,
    private val categoryService: CategoryService,
) {
    @GetMapping
    fun rules(user: UserInfo, @PathVariable groupId: ObjectId): List<CategoryRuleResponse> {
        groupAccessGuard.requireMember(user, groupId)
        val categories = categoryService.getCategoriesByGroup(groupId).associateBy { it.id }
        return categoryRuleService.rules(groupId).mapNotNull { rule ->
            categories[rule.categoryId]?.let { category ->
                CategoryRuleResponse(rule.id, rule.merchantKey, rule.categoryId, category.name, category.icon ?: DEFAULT_CATEGORY_ICON)
            }
        }
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun forget(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable ruleId: ObjectId) {
        groupAccessGuard.requireMember(user, groupId)
        if (!categoryRuleService.forget(groupId, ruleId)) throw ApiException(ApiErrorCode.NOT_FOUND, CATEGORY_RULE_NOT_FOUND_KEY)
    }
}
