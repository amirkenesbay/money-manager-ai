package ai.moneymanager.chat.transition.category

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.transition.common.confirmFlow
import ai.moneymanager.chat.transition.common.refreshCategoryList
import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.chat.transition.common.simpleTransitionWithAction
import ai.moneymanager.chat.transition.common.textInputFlow
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
            context.currentCategory = context.categories.find { category ->
                val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                "$icon ${category.name}" == buttonText
            }
        }
        then { to = MoneyManagerState.CATEGORY_ACTIONS }
    }

    textInputFlow(
        flowName = "edit category name",
        sourceState = MoneyManagerState.CATEGORY_ACTIONS,
        editState = MoneyManagerState.CATEGORY_EDIT_NAME,
        startButton = MoneyManagerButtonType.EDIT_CATEGORY,
        onSave = {
            val newName = update.message.text ?: return@textInputFlow
            val categoryId = context.currentCategory?.id ?: return@textInputFlow

            val updatedCategory = categoryService.updateCategoryName(categoryId, newName)
            if (updatedCategory != null) {
                context.currentCategory = updatedCategory
                context.renameConfirmation = "✅ Название изменено на «${updatedCategory.name}»"
                context.textInputResponse = true
                context.refreshCategoryList(categoryService)
            }
        }
    )

    simpleTransitionWithAction("Start edit category icon",
        MoneyManagerState.CATEGORY_ACTIONS, MoneyManagerButtonType.EDIT_CATEGORY_ICON, MoneyManagerState.CATEGORY_EDIT_ICON
    ) { context.manualTextInputActive = true }

    simpleTransitionWithAction("Cancel edit category icon",
        MoneyManagerState.CATEGORY_EDIT_ICON, MoneyManagerButtonType.CANCEL, MoneyManagerState.CATEGORY_ACTIONS
    ) { context.manualTextInputActive = false }

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
                context.refreshCategoryList(categoryService)
            }
        }
        then { to = MoneyManagerState.CATEGORY_ACTIONS }
    }

    transition {
        name = "Reject non-emoji icon input"
        condition {
            from = MoneyManagerState.CATEGORY_EDIT_ICON
            eventType = EventType.TEXT
        }
        action { context.iconInputError = true }
        then { to = MoneyManagerState.CATEGORY_EDIT_ICON }
    }

    confirmFlow(
        flowName = "delete category",
        sourceState = MoneyManagerState.CATEGORY_ACTIONS,
        confirmState = MoneyManagerState.CATEGORY_DELETE_CONFIRM,
        returnState = MoneyManagerState.CATEGORY_LIST,
        triggerButton = MoneyManagerButtonType.DELETE_CATEGORY_BUTTON,
        onConfirm = {
            val categoryId = context.currentCategory?.id ?: return@confirmFlow
            val deleted = categoryService.deleteCategory(categoryId)
            if (deleted) {
                context.refreshCategoryList(categoryService)
                context.currentCategory = null
            }
        }
    )

    simpleTransition("Back to category list from actions",
        MoneyManagerState.CATEGORY_ACTIONS, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.CATEGORY_LIST)
}
