package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryActionsTransitions(
    categoryService: CategoryService
) {
    transition {
        name = "Select category from list"

        condition {
            from = MoneyManagerState.CATEGORY_LIST
            button = MoneyManagerButtonType.CATEGORY_ITEM
        }

        action {
            val buttonText = buttonText ?: return@action

            // Button text format: "icon name" (e.g., "🛒 Продукты")
            // Find the category that matches this button text
            context.currentCategory = context.categories.find { category ->
                val icon = category.icon ?: "📌"
                val expectedText = "$icon ${category.name}"
                expectedText == buttonText
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    transition {
        name = "Start edit category"

        condition {
            from = MoneyManagerState.CATEGORY_ACTIONS
            button = MoneyManagerButtonType.EDIT_CATEGORY
        }

        action {
            context.manualTextInputActive = true
        }

        then {
            to = MoneyManagerState.CATEGORY_EDIT_NAME
        }
    }

    transition {
        name = "Cancel edit category"

        condition {
            from = MoneyManagerState.CATEGORY_EDIT_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    transition {
        name = "Save category new name"

        condition {
            from = MoneyManagerState.CATEGORY_EDIT_NAME
            eventType = EventType.TEXT
        }

        action {
            context.manualTextInputActive = false
            val newName = update.message.text ?: return@action
            val categoryId = context.currentCategory?.id ?: return@action

            val updatedCategory = categoryService.updateCategoryName(categoryId, newName)
            if (updatedCategory != null) {
                context.currentCategory = updatedCategory
                context.renameConfirmation = "✅ Название изменено на «${updatedCategory.name}»"
                context.textInputResponse = true

                val activeGroupId = context.userInfo?.activeGroupId
                val categoryType = context.categoryTypeInput
                if (activeGroupId != null && categoryType != null) {
                    context.categories = categoryService.getCategoriesByGroupAndType(activeGroupId, categoryType)
                }
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    transition {
        name = "Start edit category icon"

        condition {
            from = MoneyManagerState.CATEGORY_ACTIONS
            button = MoneyManagerButtonType.EDIT_CATEGORY_ICON
        }

        action {
            context.manualTextInputActive = true
        }

        then {
            to = MoneyManagerState.CATEGORY_EDIT_ICON
        }
    }

    transition {
        name = "Cancel edit category icon"

        condition {
            from = MoneyManagerState.CATEGORY_EDIT_ICON
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    transition {
        name = "Save category new icon"

        condition {
            from = MoneyManagerState.CATEGORY_EDIT_ICON
            eventType = EventType.TEXT
            guard {
                val text = update.message?.text?.trim() ?: return@guard false
                text.isNotEmpty() && !text.any { it in 'a'..'z' || it in 'A'..'Z' || it in 'а'..'я' || it in 'А'..'Я' || it in '0'..'9' }
            }
        }

        action {
            context.manualTextInputActive = false
            context.iconInputError = false
            val newIcon = update.message.text?.trim() ?: return@action
            val categoryId = context.currentCategory?.id ?: return@action

            val updatedCategory = categoryService.updateCategoryIcon(categoryId, newIcon)
            if (updatedCategory != null) {
                context.currentCategory = updatedCategory
                context.textInputResponse = true

                val activeGroupId = context.userInfo?.activeGroupId
                val categoryType = context.categoryTypeInput
                if (activeGroupId != null && categoryType != null) {
                    context.categories = categoryService.getCategoriesByGroupAndType(activeGroupId, categoryType)
                }
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    transition {
        name = "Reject non-emoji icon input"

        condition {
            from = MoneyManagerState.CATEGORY_EDIT_ICON
            eventType = EventType.TEXT
        }

        action {
            context.iconInputError = true
        }

        then {
            to = MoneyManagerState.CATEGORY_EDIT_ICON
        }
    }

    transition {
        name = "Start delete category"

        condition {
            from = MoneyManagerState.CATEGORY_ACTIONS
            button = MoneyManagerButtonType.DELETE_CATEGORY_BUTTON
        }

        then {
            to = MoneyManagerState.CATEGORY_DELETE_CONFIRM
        }
    }

    transition {
        name = "Confirm delete category"

        condition {
            from = MoneyManagerState.CATEGORY_DELETE_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }

        action {
            val categoryId = context.currentCategory?.id ?: return@action
            val categoryType = context.currentCategory?.type

            val deleted = categoryService.deleteCategory(categoryId)
            if (deleted) {
                val activeGroupId = context.userInfo?.activeGroupId
                if (activeGroupId != null && categoryType != null) {
                    context.categories = categoryService.getCategoriesByGroupAndType(activeGroupId, categoryType)
                }
                context.currentCategory = null
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST
        }
    }

    transition {
        name = "Cancel delete category"

        condition {
            from = MoneyManagerState.CATEGORY_DELETE_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    transition {
        name = "Back to category list from actions"

        condition {
            from = MoneyManagerState.CATEGORY_ACTIONS
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST
        }
    }
}