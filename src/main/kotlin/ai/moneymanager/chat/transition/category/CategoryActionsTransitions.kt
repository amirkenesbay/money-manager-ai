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

            val categoryNumber = buttonText.substringBefore(".").trim().toIntOrNull() ?: return@action
            val categoryIndex = categoryNumber - 1

            if (categoryIndex < 0 || categoryIndex >= context.categories.size) return@action

            context.currentCategory = context.categories[categoryIndex]
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

                val activeGroupId = context.userInfo?.activeGroupId
                if (activeGroupId != null) {
                    context.categories = categoryService.getCategoriesByGroup(activeGroupId)
                }
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
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

            val deleted = categoryService.deleteCategory(categoryId)
            if (deleted) {
                val activeGroupId = context.userInfo?.activeGroupId
                if (activeGroupId != null) {
                    context.categories = categoryService.getCategoriesByGroup(activeGroupId)
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