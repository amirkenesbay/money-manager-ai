package ai.moneymanager.web.category

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryCreationResult
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.CategoryUpdateResult
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.MAX_CATEGORY_NAME_LENGTH
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.access.GroupCategoryLookup
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.limitReached
import ai.moneymanager.web.input.normalizeIcon
import ai.moneymanager.web.input.requireName
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ai.moneymanager.domain.model.BudgetLimitResult
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.badRequest
import org.springframework.web.bind.annotation.PutMapping
import java.math.BigDecimal

data class CategoryResponse(
    val id: ObjectId,
    val name: String,
    val icon: String,
    val type: CategoryType,
    val monthlyLimit: BigDecimal?,
)

data class CategoryLimitRequest(
    val amount: BigDecimal?,
)

data class CreateCategoryRequest(
    val name: String?,
    val icon: String?,
    val type: CategoryType,
)

data class UpdateCategoryRequest(
    val name: String?,
    val icon: String?,
)

private const val BUDGET_AMOUNT_KEY = "${API_ERROR_KEY_PREFIX}budget_amount"
private const val BUDGET_INCOME_KEY = "${API_ERROR_KEY_PREFIX}budget_income"

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/categories")
class CategoryController(
    private val categoryService: CategoryService,
    private val groupAccessGuard: GroupAccessGuard,
    private val groupCategoryLookup: GroupCategoryLookup,
    private val subscriptionLimitsService: SubscriptionLimitsService,
) {
    @GetMapping
    fun categories(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @RequestParam(required = false) type: CategoryType?,
    ): List<CategoryResponse> {
        groupAccessGuard.requireMember(user, groupId)
        val categories = type?.let { categoryService.getCategoriesByGroupAndType(groupId, it) }
            ?: categoryService.getCategoriesByGroup(groupId)
        return categories.map { it.toResponse() }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: CreateCategoryRequest): CategoryResponse {
        groupAccessGuard.requireMember(user, groupId)
        val name = requireName(request.name, MAX_CATEGORY_NAME_LENGTH)
        val maxPerType = subscriptionLimitsService.maxCategoriesPerType(user.hasActivePaidSubscription())
        return when (val result = categoryService.createCategory(checkNotNull(user.telegramUserId), name, normalizeIcon(request.icon), request.type, groupId, maxPerType)) {
            is CategoryCreationResult.Created -> result.category.toResponse()
            CategoryCreationResult.Duplicate -> throw duplicate(name)
            is CategoryCreationResult.LimitReached -> throw limitReached(ApiErrorCode.CATEGORY_LIMIT_REACHED, result.limit)
        }
    }

    @PatchMapping("/{categoryId}")
    fun update(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @PathVariable categoryId: ObjectId,
        @RequestBody request: UpdateCategoryRequest,
    ): CategoryResponse {
        groupAccessGuard.requireMember(user, groupId)
        val category = groupCategoryLookup.requireCategory(groupId, categoryId)
        val name = request.name?.let { requireName(it, MAX_CATEGORY_NAME_LENGTH) } ?: category.name
        val icon = normalizeIcon(request.icon) ?: category.icon
        return when (val result = categoryService.updateCategory(checkNotNull(user.telegramUserId), categoryId, name, icon)) {
            is CategoryUpdateResult.Updated -> result.category.toResponse()
            CategoryUpdateResult.Duplicate -> throw duplicate(name)
            CategoryUpdateResult.NotFound -> throw groupCategoryLookup.notFound()
        }
    }

    @PutMapping("/{categoryId}/limit")
    fun setLimit(
        user: UserInfo,
        @PathVariable groupId: ObjectId,
        @PathVariable categoryId: ObjectId,
        @RequestBody request: CategoryLimitRequest,
    ): CategoryResponse {
        groupAccessGuard.requireMember(user, groupId)
        groupCategoryLookup.requireCategory(groupId, categoryId)
        if (request.amount != null && request.amount.signum() <= 0) throw badRequest(BUDGET_AMOUNT_KEY)
        val maxBudgets = subscriptionLimitsService.maxBudgets(user.hasActivePaidSubscription())
        return when (val result = categoryService.setMonthlyLimit(categoryId, request.amount, maxBudgets)) {
            is BudgetLimitResult.Updated -> result.category.toResponse()
            is BudgetLimitResult.LimitReached -> throw limitReached(ApiErrorCode.BUDGET_LIMIT_REACHED, result.limit)
            BudgetLimitResult.NotExpense -> throw badRequest(BUDGET_INCOME_KEY)
            BudgetLimitResult.NotFound -> throw groupCategoryLookup.notFound()
        }
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: UserInfo, @PathVariable groupId: ObjectId, @PathVariable categoryId: ObjectId) {
        groupAccessGuard.requireMember(user, groupId)
        groupCategoryLookup.requireCategory(groupId, categoryId)
        if (!categoryService.deleteCategory(checkNotNull(user.telegramUserId), categoryId)) throw groupCategoryLookup.notFound()
    }

    private fun duplicate(name: String) = ApiException(ApiErrorCode.CATEGORY_DUPLICATE, messageArgs = listOf(name))

    private fun Category.toResponse() = CategoryResponse(
        id = checkNotNull(id),
        name = name,
        icon = icon ?: DEFAULT_CATEGORY_ICON,
        type = type,
        monthlyLimit = monthlyLimit,
    )
}
