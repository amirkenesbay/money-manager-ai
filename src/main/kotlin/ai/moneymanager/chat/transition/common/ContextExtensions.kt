package ai.moneymanager.chat.transition.common

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.GroupCreationResult
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.service.CategoryService

fun MoneyManagerContext.handleGroupCreated(result: GroupCreationResult) {
    groupNameDuplicateError = false
    groupCreationLimitReached = null

    when (result) {
        is GroupCreationResult.Created -> {
            currentGroup = result.group
            userInfo = userInfo?.copy(
                activeGroupId = result.group.id,
                groupIds = userInfo?.groupIds?.plus(result.group.id!!) ?: setOf(result.group.id!!)
            )
        }
        is GroupCreationResult.Duplicate -> groupNameDuplicateError = true
        is GroupCreationResult.LimitReached -> groupCreationLimitReached = result.limit
    }
}

fun MoneyManagerContext.refreshCategoryList(categoryService: CategoryService) {
    val activeGroupId = userInfo?.activeGroupId
    val categoryType = categoryTypeInput
    if (activeGroupId != null && categoryType != null) {
        categories = categoryService.getCategoriesByGroupAndType(activeGroupId, categoryType)
    }
}

fun MoneyManagerContext.loadCategoriesByType(
    type: CategoryType,
    categoryService: CategoryService
) {
    val activeGroupId = userInfo?.activeGroupId
    if (activeGroupId != null) {
        categoryTypeInput = type
        categories = categoryService.getCategoriesByGroupAndType(activeGroupId, type)
    }
}

fun parseGroupNameFromButton(buttonText: String): String {
    return buttonText
        .removePrefix("✅ 👑 ")
        .removePrefix("✅ ")
        .removePrefix("👑 ")
        .trim()
}
