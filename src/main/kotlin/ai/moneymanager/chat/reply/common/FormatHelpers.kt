package ai.moneymanager.chat.reply.common

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.UserInfo

fun formatUserDisplayName(userInfo: UserInfo?, fallbackId: Long): String {
    if (userInfo == null) return "ID $fallbackId"

    val fullName = listOfNotNull(userInfo.firstName, userInfo.lastName)
        .joinToString(" ")

    return when {
        fullName.isNotEmpty() -> fullName
        !userInfo.username.isNullOrEmpty() -> "@${userInfo.username}"
        else -> "ID $fallbackId"
    }
}

fun categoryTypeLabel(type: CategoryType?, form: CategoryTypeForm = CategoryTypeForm.GENITIVE): String {
    return when (form) {
        CategoryTypeForm.GENITIVE -> when (type) {
            CategoryType.EXPENSE -> "расхода"
            CategoryType.INCOME -> "дохода"
            else -> "категории"
        }
        CategoryTypeForm.GENITIVE_PLURAL -> when (type) {
            CategoryType.EXPENSE -> "расходов"
            CategoryType.INCOME -> "доходов"
            else -> ""
        }
    }
}

enum class CategoryTypeForm {
    GENITIVE,
    GENITIVE_PLURAL
}
