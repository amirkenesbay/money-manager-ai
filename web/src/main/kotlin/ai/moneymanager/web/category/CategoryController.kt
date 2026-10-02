package ai.moneymanager.web.category

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.CategoryService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class CategoryResponse(
    val id: ObjectId,
    val name: String,
    val icon: String,
    val type: CategoryType,
)

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/categories")
class CategoryController(
    private val categoryService: CategoryService,
    private val groupAccessGuard: GroupAccessGuard,
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

    private fun Category.toResponse() = CategoryResponse(
        id = checkNotNull(id),
        name = name,
        icon = icon ?: DEFAULT_CATEGORY_ICON,
        type = type,
    )
}
