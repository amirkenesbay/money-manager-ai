package ai.moneymanager.chat.transition.category

import ai.moneymanager.chat.transition.common.simpleTransition
import ai.moneymanager.chat.transition.common.simpleTransitionWithAction
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.QuickTemplates
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("CreateCategoryTransitions")
private const val MAX_CATEGORY_NAME_LENGTH = 50

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createCategoryTransitions(
    categoryService: CategoryService
) {
    transition {
        name = "Start category creation - check active group exists"
        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.CREATE_CATEGORY
            guard { context.userInfo?.activeGroupId != null }
        }
        then { to = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE }
    }

    transition {
        name = "Start category creation - no active group warning"
        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.CREATE_CATEGORY
            guard { context.userInfo?.activeGroupId == null }
        }
        then { to = MoneyManagerState.CATEGORY_NO_GROUP_WARNING }
    }

    simpleTransition("Go to group creation from no group warning",
        MoneyManagerState.CATEGORY_NO_GROUP_WARNING, MoneyManagerButtonType.CREATE_GROUP, MoneyManagerState.GROUP_MANAGEMENT)

    simpleTransition("Back to category management from no group warning",
        MoneyManagerState.CATEGORY_NO_GROUP_WARNING, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.CATEGORY_MANAGEMENT)

    simpleTransitionWithAction("Select expense category type",
        MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE, MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE, MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
    ) {
        context.categoryTypeInput = CategoryType.EXPENSE
        context.manualTextInputActive = true
    }

    simpleTransitionWithAction("Select income category type",
        MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE, MoneyManagerButtonType.CATEGORY_TYPE_INCOME, MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
    ) {
        context.categoryTypeInput = CategoryType.INCOME
        context.manualTextInputActive = true
    }

    simpleTransition("Cancel category type selection",
        MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE, MoneyManagerButtonType.CANCEL, MoneyManagerState.CATEGORY_MANAGEMENT)

    simpleTransitionWithAction("Cancel category creation",
        MoneyManagerState.CATEGORY_CREATE_ENTER_NAME, MoneyManagerButtonType.CANCEL, MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
    ) {
        context.categoryTypeInput = null
        context.manualTextInputActive = false
        context.isQuickCategoryCreation = false
    }

    QuickTemplates.ALL_CATEGORIES.forEach { template ->
        transition {
            name = "Quick create: ${template.name}"
            condition {
                from = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
                button = template.buttonType
            }
            action {
                context.categoryNameInput = template.name
                context.categoryIconInput = template.icon
                context.isQuickCategoryCreation = true
                context.manualTextInputActive = false

                val activeGroupId = context.userInfo?.activeGroupId
                val categoryType = context.categoryTypeInput ?: CategoryType.EXPENSE
                log.info("Quick creating category: name='${template.name}', icon='${template.icon}', type=$categoryType, activeGroupId=$activeGroupId")

                if (activeGroupId != null) {
                    context.currentCategory = categoryService.createCategory(
                        name = template.name,
                        icon = template.icon,
                        type = categoryType,
                        groupId = activeGroupId
                    )
                } else {
                    log.warn("Skipping quick category creation - activeGroupId is null! userInfo=${context.userInfo}")
                }

                context.categoryTypeInput = null
            }
            then { to = MoneyManagerState.CATEGORY_CREATE_RESULT }
        }
    }

    transition {
        name = "Create category with name"
        condition {
            from = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }
        action {
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
                context.currentCategory = categoryService.createCategory(
                    name = categoryName,
                    icon = null,
                    type = categoryType,
                    groupId = activeGroupId
                )
            } else {
                log.warn("Skipping category creation - activeGroupId is null! userInfo=${context.userInfo}")
            }

            context.categoryTypeInput = null
        }
        then { to = MoneyManagerState.CATEGORY_CREATE_RESULT }
    }

    simpleTransitionWithAction("Create another category from result",
        MoneyManagerState.CATEGORY_CREATE_RESULT, MoneyManagerButtonType.CREATE_CATEGORY, MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
    ) { context.isQuickCategoryCreation = false }

    simpleTransition("Back to category management from create result",
        MoneyManagerState.CATEGORY_CREATE_RESULT, MoneyManagerButtonType.BACK_TO_MENU, MoneyManagerState.CATEGORY_MANAGEMENT)
}
