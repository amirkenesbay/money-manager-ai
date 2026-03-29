package ai.moneymanager.chat.transition.common

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.service.CategoryService

fun MoneyManagerContext.handleGroupCreated(createdGroup: MoneyGroup?) {
    if (createdGroup != null) {
        currentGroup = createdGroup
        userInfo = userInfo?.copy(
            activeGroupId = createdGroup.id,
            groupIds = userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
        )
        groupNameDuplicateError = false
    } else {
        groupNameDuplicateError = true
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
