
package ai.moneymanager.chat.transition.category

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import org.slf4j.LoggerFactory

private const val MAX_CATEGORY_NAME_LENGTH = 50

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createCategoryTransitions(
    categoryService: CategoryService
) {
    transition {
        name = "Start category creation - check active group exists"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.CREATE_CATEGORY
            guard {
                context.userInfo?.activeGroupId != null
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
        }
    }

    transition {
        name = "Start category creation - no active group warning"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.CREATE_CATEGORY
            guard {
                context.userInfo?.activeGroupId == null
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_NO_GROUP_WARNING
        }
    }

    transition {
        name = "Go to group creation from no group warning"

        condition {
            from = MoneyManagerState.CATEGORY_NO_GROUP_WARNING
            button = MoneyManagerButtonType.CREATE_GROUP
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }

    transition {
        name = "Back to category management from no group warning"

        condition {
            from = MoneyManagerState.CATEGORY_NO_GROUP_WARNING
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }

    transition {
        name = "Select expense category type"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
        }

        action {
            context.categoryTypeInput = CategoryType.EXPENSE
            context.manualTextInputActive = true
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
        }
    }

    transition {
        name = "Select income category type"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
        }

        action {
            context.categoryTypeInput = CategoryType.INCOME
            context.manualTextInputActive = true
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
        }
    }

    transition {
        name = "Cancel category type selection"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }

    transition {
        name = "Cancel category creation"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.categoryTypeInput = null
            context.manualTextInputActive = false
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
        }
    }

    transition {
        name = "Create category with name"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            val log = LoggerFactory.getLogger("CreateCategoryTransitions")
            context.manualTextInputActive = false
            val categoryName = update.message.text?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.take(MAX_CATEGORY_NAME_LENGTH)
                ?: "Новая категория"
            context.categoryNameInput = categoryName

            val activeGroupId = context.userInfo?.activeGroupId
            val categoryType = context.categoryTypeInput ?: CategoryType.EXPENSE
            log.info("Creating category: name='$categoryName', type=$categoryType, activeGroupId=$activeGroupId")

            if (activeGroupId != null) {
                val createdCategory = categoryService.createCategory(
                    name = categoryName,
                    icon = null,
                    type = categoryType,
                    groupId = activeGroupId
                )
                context.currentCategory = createdCategory
                log.info("Category creation result: ${if (createdCategory != null) "success, id=${createdCategory.id}" else "null (duplicate?)"}")
            } else {
                log.warn("Skipping category creation - activeGroupId is null! userInfo=${context.userInfo}")
            }

            context.categoryTypeInput = null
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_RESULT
        }
    }

    transition {
        name = "Create another category from result"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_RESULT
            button = MoneyManagerButtonType.CREATE_CATEGORY
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
        }
    }

    transition {
        name = "Back to category management from create result"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_RESULT
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}