package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DeleteAllCategoriesTransitions")

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.deleteAllCategoriesTransitions(
    categoryService: CategoryService
) {
    transition {
        name = "Start delete all categories"

        condition {
            from = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE
            button = MoneyManagerButtonType.DELETE_ALL_CATEGORIES
        }

        action {
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                context.categories = categoryService.getCategoriesByGroup(activeGroupId)
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM
        }
    }

    transition {
        name = "Confirm delete all categories"

        condition {
            from = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }

        action {
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                val deletedCount = categoryService.deleteAllCategoriesForGroup(activeGroupId)
                context.categories = emptyList()
                context.currentCategory = null
                log.info("Deleted $deletedCount categories for group $activeGroupId")
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }

    transition {
        name = "Cancel delete all categories"

        condition {
            from = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.categories = emptyList()
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}