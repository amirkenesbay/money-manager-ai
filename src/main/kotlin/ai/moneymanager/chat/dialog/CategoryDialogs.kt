package ai.moneymanager.chat.dialog

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryDialogTransitions(
    categoryService: CategoryService
) {
    // Открыть меню управления категориями
    openCategoryManagementTransition()

    // Создание категории
    createCategoryDialogTransitions(categoryService)

    // Просмотр списка категорий
    viewCategoriesListTransition(categoryService)

    // Работа с выбранной категорией
    categoryActionsTransitions(categoryService)

    // Массовое удаление всех категорий
    deleteAllCategoriesTransitions(categoryService)

    // Возврат в главное меню
    backToMenuFromCategoriesTransition()
}

/**
 * Открыть меню управления категориями
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openCategoryManagementTransition() {
    transition {
        name = "Open category management"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.CATEGORIES
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}

/**
 * Диалог создания категории
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createCategoryDialogTransitions(
    categoryService: CategoryService
) {
    // Начать создание категории - проверка наличия активной группы
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

    // Попытка создать категорию без активной группы
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

    // Переход к созданию группы из предупреждения
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

    // Возврат в меню категорий из предупреждения
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

    // Выбран тип "Расход"
    transition {
        name = "Select expense category type"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
        }

        action {
            context.categoryTypeInput = CategoryType.EXPENSE
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
        }
    }

    // Выбран тип "Доход"
    transition {
        name = "Select income category type"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
            button = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
        }

        action {
            context.categoryTypeInput = CategoryType.INCOME
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
        }
    }

    // Отмена выбора типа
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

    // Отмена создания категории
    transition {
        name = "Cancel category creation"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            // Очищаем выбранный тип
            context.categoryTypeInput = null
        }

        then {
            to = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE
        }
    }

    // Получить название категории и создать её
    transition {
        name = "Create category with name"

        condition {
            from = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            val categoryName = update.message.text ?: "Новая категория"
            context.categoryNameInput = categoryName

            // Получаем активную группу и выбранный тип
            val activeGroupId = context.userInfo?.activeGroupId
            val categoryType = context.categoryTypeInput ?: CategoryType.EXPENSE

            if (activeGroupId != null) {
                val createdCategory = categoryService.createCategory(
                    name = categoryName,
                    icon = null,
                    type = categoryType,
                    groupId = activeGroupId
                )

                if (createdCategory != null) {
                    context.currentCategory = createdCategory
                } else {
                    // Категория с таким названием уже существует
                    // Сохраняем информацию об ошибке (можно показать пользователю)
                    context.currentCategory = null
                }
            }

            // Очищаем временные данные
            context.categoryTypeInput = null
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}

/**
 * Просмотр списка категорий
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.viewCategoriesListTransition(
    categoryService: CategoryService
) {
    transition {
        name = "View categories list"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.MY_CATEGORIES
        }

        action {
            // Получаем активную группу
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                val categories = categoryService.getCategoriesByGroup(activeGroupId)
                context.categories = categories
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_LIST
        }
    }
}

/**
 * Действия над выбранной категорией
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.categoryActionsTransitions(
    categoryService: CategoryService
) {
    // Клик по категории из списка
    transition {
        name = "Select category from list"

        condition {
            from = MoneyManagerState.CATEGORY_LIST
            button = MoneyManagerButtonType.CATEGORY_ITEM
        }

        action {
            val buttonText = buttonText ?: return@action

            // Извлекаем номер категории из текста кнопки (например, "1. 🛒 📉 Продукты" -> "1")
            val categoryNumber = buttonText.substringBefore(".").trim().toIntOrNull() ?: return@action
            val categoryIndex = categoryNumber - 1

            // Проверяем валидность индекса
            if (categoryIndex < 0 || categoryIndex >= context.categories.size) {
                return@action
            }

            // Получаем выбранную категорию
            val selectedCategory = context.categories[categoryIndex]
            context.currentCategory = selectedCategory
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    // Редактировать категорию
    transition {
        name = "Start edit category"

        condition {
            from = MoneyManagerState.CATEGORY_ACTIONS
            button = MoneyManagerButtonType.EDIT_CATEGORY
        }

        then {
            to = MoneyManagerState.CATEGORY_EDIT_NAME
        }
    }

    // Отмена редактирования
    transition {
        name = "Cancel edit category"

        condition {
            from = MoneyManagerState.CATEGORY_EDIT_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.CATEGORY_ACTIONS
        }
    }

    // Сохранить новое название
    transition {
        name = "Save category new name"

        condition {
            from = MoneyManagerState.CATEGORY_EDIT_NAME
            eventType = EventType.TEXT
        }

        action {
            val newName = update.message.text ?: return@action
            val categoryId = context.currentCategory?.id ?: return@action

            val updatedCategory = categoryService.updateCategoryName(categoryId, newName)
            if (updatedCategory != null) {
                context.currentCategory = updatedCategory

                // Обновляем список категорий
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

    // Начать удаление категории
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

    // Подтверждение удаления
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
                // Обновляем список категорий
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

    // Отмена удаления
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

    // Возврат из меню действий к списку
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

/**
 * Массовое удаление всех категорий группы
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.deleteAllCategoriesTransitions(
    categoryService: CategoryService
) {
    // Открыть подтверждение удаления всех категорий
    transition {
        name = "Start delete all categories"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.DELETE_ALL_CATEGORIES
        }

        action {
            // Загружаем текущие категории для отображения в подтверждении
            val activeGroupId = context.userInfo?.activeGroupId
            if (activeGroupId != null) {
                val categories = categoryService.getCategoriesByGroup(activeGroupId)
                context.categories = categories
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM
        }
    }

    // Подтверждение удаления всех категорий
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

                // Очищаем контекст
                context.categories = emptyList()
                context.currentCategory = null

                println("🗑 Deleted $deletedCount categories for group $activeGroupId")
            }
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }

    // Отмена удаления всех категорий
    transition {
        name = "Cancel delete all categories"

        condition {
            from = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            // Очищаем загруженные категории
            context.categories = emptyList()
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}

/**
 * Возврат в главное меню
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.backToMenuFromCategoriesTransition() {
    // Возврат из управления категориями
    transition {
        name = "Back to menu from category management"

        condition {
            from = MoneyManagerState.CATEGORY_MANAGEMENT
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    // Возврат из списка категорий
    transition {
        name = "Back to category management from category list"

        condition {
            from = MoneyManagerState.CATEGORY_LIST
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.CATEGORY_MANAGEMENT
        }
    }
}