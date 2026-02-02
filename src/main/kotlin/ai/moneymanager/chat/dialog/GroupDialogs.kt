package ai.moneymanager.chat.dialog

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.GroupService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.groupDialogTransitions(
    groupService: GroupService,
    categoryService: ai.moneymanager.service.CategoryService
) {
    // Открыть меню управления группами
    openGroupManagementTransition()

    // Создание группы
    createGroupDialogTransitions(groupService)

    // Приглашение в группу
    inviteGroupTransitions(groupService)

    // Просмотр списка групп
    viewGroupsListTransition(groupService)

    // Просмотр участников группы
    viewGroupMembersTransition(groupService)

    // Редактирование группы
    editGroupTransitions(groupService)

    // Удаление группы
    deleteGroupTransitions(groupService, categoryService)

    // Возврат в главное меню
    backToMenuTransition()
}

/**
 * Открыть меню управления группами
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.openGroupManagementTransition() {
    transition {
        name = "Open group management"

        condition {
            from = MoneyManagerState.MENU
            button = MoneyManagerButtonType.SHARED_ACCOUNTING
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }
}

/**
 * Диалог создания группы
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createGroupDialogTransitions(
    groupService: GroupService
) {
    // Начать создание группы
    transition {
        name = "Start group creation"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.CREATE_GROUP
        }

        then {
            to = MoneyManagerState.GROUP_CREATE_ENTER_NAME
        }
    }

    // Отмена создания группы
    transition {
        name = "Cancel group creation"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }

    // Быстрое создание группы "Семья"
    transition {
        name = "Quick create group: Family"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.QUICK_GROUP_FAMILY
        }

        action {
            val groupName = "Семья"
            context.groupNameInput = groupName
            context.isQuickGroupCreation = true

            val userId = user.id
            val createdGroup = groupService.createGroup(userId, groupName)
            context.currentGroup = createdGroup

            context.userInfo = context.userInfo?.copy(
                activeGroupId = createdGroup.id,
                groupIds = context.userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
            )
        }

        then {
            to = MoneyManagerState.FINANCE_OPERATION_EXPENSE_OR_INCOME
        }
    }

    // Быстрое создание группы "С другом"
    transition {
        name = "Quick create group: With Friend"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.QUICK_GROUP_WITH_FRIEND
        }

        action {
            val groupName = "С другом"
            context.groupNameInput = groupName
            context.isQuickGroupCreation = true

            val userId = user.id
            val createdGroup = groupService.createGroup(userId, groupName)
            context.currentGroup = createdGroup

            context.userInfo = context.userInfo?.copy(
                activeGroupId = createdGroup.id,
                groupIds = context.userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
            )
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    // Быстрое создание группы "Работа"
    transition {
        name = "Quick create group: Work"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.QUICK_GROUP_WORK
        }

        action {
            val groupName = "Работа"
            context.groupNameInput = groupName
            context.isQuickGroupCreation = true

            val userId = user.id
            val createdGroup = groupService.createGroup(userId, groupName)
            context.currentGroup = createdGroup

            context.userInfo = context.userInfo?.copy(
                activeGroupId = createdGroup.id,
                groupIds = context.userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
            )
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    // Быстрое создание группы "Поездка"
    transition {
        name = "Quick create group: Trip"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            button = MoneyManagerButtonType.QUICK_GROUP_TRIP
        }

        action {
            val groupName = "Поездка"
            context.groupNameInput = groupName
            context.isQuickGroupCreation = true

            val userId = user.id
            val createdGroup = groupService.createGroup(userId, groupName)
            context.currentGroup = createdGroup

            context.userInfo = context.userInfo?.copy(
                activeGroupId = createdGroup.id,
                groupIds = context.userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
            )
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    // Получить название группы и создать её (ручной ввод)
    transition {
        name = "Create group with name"

        condition {
            from = MoneyManagerState.GROUP_CREATE_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            val groupName = update.message.text ?: "Моя группа"
            context.groupNameInput = groupName
            context.isQuickGroupCreation = false

            val userId = user.id
            val createdGroup = groupService.createGroup(userId, groupName)
            context.currentGroup = createdGroup

            // Обновляем userInfo чтобы activeGroupId был актуальным
            context.userInfo = context.userInfo?.copy(
                activeGroupId = createdGroup.id,
                groupIds = context.userInfo?.groupIds?.plus(createdGroup.id!!) ?: setOf(createdGroup.id!!)
            )
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }
}

/**
 * Диалог приглашения в группу
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.inviteGroupTransitions(
    groupService: GroupService
) {
    // Открыть выбор группы для получения ссылки приглашения
    transition {
        name = "Open invite group selection"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.INVITE_TO_GROUP
        }

        action {
            val userId = user.id
            val groups = groupService.getUserGroups(userId)
            context.userGroups = groups
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SELECT
        }
    }

    // Выбрать группу для получения ссылки приглашения
    transition {
        name = "Select group to get invite link"

        condition {
            from = MoneyManagerState.GROUP_INVITE_SELECT
            button = MoneyManagerButtonType.INVITE_TO_GROUP
        }

        action {
            val buttonText = buttonText ?: return@action
            val userInfo = context.userInfo

            // Извлекаем номер группы
            val groupNumber = buttonText.trim().toIntOrNull() ?: return@action

            // Фильтруем только группы, где пользователь - владелец
            val ownedGroups = context.userGroups.filter { it.ownerId == userInfo?.telegramUserId }
            val groupIndex = groupNumber - 1

            // Проверяем валидность индекса
            if (groupIndex < 0 || groupIndex >= ownedGroups.size) {
                return@action
            }

            // Устанавливаем выбранную группу
            context.currentGroup = ownedGroups[groupIndex]
            // Сбрасываем флаг быстрого создания для корректного отображения
            context.isQuickGroupCreation = false
        }

        then {
            to = MoneyManagerState.GROUP_INVITE_SHOW
        }
    }

    // Отмена выбора группы для приглашения
    transition {
        name = "Cancel invite selection"

        condition {
            from = MoneyManagerState.GROUP_INVITE_SELECT
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }
}

/**
 * Просмотр списка групп
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.viewGroupsListTransition(
    groupService: GroupService
) {
    transition {
        name = "View groups list"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.MY_GROUPS
        }

        action {
            val userId = user.id
            val groups = groupService.getUserGroups(userId)
            context.userGroups = groups

            // Получаем активную группу пользователя
            val activeGroup = groupService.getActiveGroup(userId)
            context.userInfo = context.userInfo?.copy(
                activeGroupId = activeGroup?.id,
                groupIds = groups.mapNotNull { it.id }.toSet()
            )
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }

    // Переключение активной группы
    transition {
        name = "Switch active group"

        condition {
            from = MoneyManagerState.GROUP_LIST
            button = MoneyManagerButtonType.SWITCH_GROUP
        }

        action {
            val userId = user.id
            val buttonText = buttonText ?: return@action

            // Извлекаем номер группы из текста кнопки (например, "✅ 1" -> "1" или "2" -> "2")
            val groupNumber = buttonText.replace("✅", "").trim().toIntOrNull() ?: return@action
            val groupIndex = groupNumber - 1

            // Проверяем валидность индекса
            if (groupIndex < 0 || groupIndex >= context.userGroups.size) {
                return@action
            }

            // Получаем выбранную группу
            val selectedGroup = context.userGroups[groupIndex]
            val groupId = selectedGroup.id ?: return@action

            // Переключаем активную группу
            groupService.switchActiveGroup(userId, groupId)

            // Обновляем контекст
            context.userInfo = context.userInfo?.copy(activeGroupId = groupId)
            context.currentGroup = selectedGroup

            // Обновляем список групп для актуальной информации
            val groups = groupService.getUserGroups(userId)
            context.userGroups = groups
        }

        then {
            // Остаемся в том же состоянии, чтобы обновить список
            to = MoneyManagerState.GROUP_LIST
        }
    }
}

/**
 * Просмотр участников группы
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.viewGroupMembersTransition(
    groupService: GroupService
) {
    transition {
        name = "View group members"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.GROUP_MEMBERS
        }

        action {
            val userId = user.id
            val activeGroup = groupService.getActiveGroup(userId)
            context.currentGroup = activeGroup
        }

        then {
            to = MoneyManagerState.GROUP_MEMBERS
        }
    }
}

/**
 * Редактирование группы
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.editGroupTransitions(
    groupService: GroupService
) {
    // Открыть выбор группы для редактирования
    transition {
        name = "Open edit group selection"

        condition {
            from = MoneyManagerState.GROUP_LIST
            button = MoneyManagerButtonType.EDIT_GROUP
        }

        then {
            to = MoneyManagerState.GROUP_EDIT_SELECT
        }
    }

    // Выбрать группу для редактирования по номеру
    transition {
        name = "Select group to edit"

        condition {
            from = MoneyManagerState.GROUP_EDIT_SELECT
            button = MoneyManagerButtonType.EDIT_GROUP
        }

        action {
            val buttonText = buttonText ?: return@action
            val userInfo = context.userInfo

            // Извлекаем номер группы
            val groupNumber = buttonText.trim().toIntOrNull() ?: return@action

            // Фильтруем только группы, где пользователь - владелец
            val ownedGroups = context.userGroups.filter { it.ownerId == userInfo?.telegramUserId }
            val groupIndex = groupNumber - 1

            // Проверяем валидность индекса
            if (groupIndex < 0 || groupIndex >= ownedGroups.size) {
                return@action
            }

            // Устанавливаем выбранную группу
            context.currentGroup = ownedGroups[groupIndex]
        }

        then {
            to = MoneyManagerState.GROUP_EDIT_ENTER_NAME
        }
    }

    // Отмена выбора группы для редактирования
    transition {
        name = "Cancel edit selection"

        condition {
            from = MoneyManagerState.GROUP_EDIT_SELECT
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }

    // Ввод нового названия группы
    transition {
        name = "Update group name"

        condition {
            from = MoneyManagerState.GROUP_EDIT_ENTER_NAME
            eventType = EventType.TEXT
        }

        action {
            val userId = user.id
            val groupId = context.currentGroup?.id ?: return@action
            val newName = update.message.text ?: return@action

            val updatedGroup = groupService.updateGroupName(userId, groupId, newName)

            if (updatedGroup != null) {
                // Обновляем контекст
                context.currentGroup = updatedGroup

                // Обновляем список групп
                val groups = groupService.getUserGroups(userId)
                context.userGroups = groups
            }
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }

    // Отмена редактирования
    transition {
        name = "Cancel edit name"

        condition {
            from = MoneyManagerState.GROUP_EDIT_ENTER_NAME
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.GROUP_EDIT_SELECT
        }
    }
}

/**
 * Удаление группы
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.deleteGroupTransitions(
    groupService: GroupService,
    categoryService: ai.moneymanager.service.CategoryService
) {
    // Открыть выбор группы для удаления
    transition {
        name = "Open delete group selection"

        condition {
            from = MoneyManagerState.GROUP_LIST
            button = MoneyManagerButtonType.DELETE_GROUP
        }

        then {
            to = MoneyManagerState.GROUP_DELETE_SELECT
        }
    }

    // Выбрать группу для удаления по номеру
    transition {
        name = "Select group to delete"

        condition {
            from = MoneyManagerState.GROUP_DELETE_SELECT
            button = MoneyManagerButtonType.DELETE_GROUP
        }

        action {
            val buttonText = buttonText ?: return@action
            val userInfo = context.userInfo

            // Извлекаем номер группы
            val groupNumber = buttonText.trim().toIntOrNull() ?: return@action

            // Фильтруем только группы, где пользователь - владелец
            val ownedGroups = context.userGroups.filter { it.ownerId == userInfo?.telegramUserId }
            val groupIndex = groupNumber - 1

            // Проверяем валидность индекса
            if (groupIndex < 0 || groupIndex >= ownedGroups.size) {
                return@action
            }

            // Устанавливаем выбранную группу
            val selectedGroup = ownedGroups[groupIndex]
            context.currentGroup = selectedGroup

            // Получаем количество категорий группы для отображения в предупреждении
            val groupId = selectedGroup.id
            if (groupId != null) {
                val categories = categoryService.getCategoriesByGroup(groupId)
                context.categoriesCountToDelete = categories.size
                println("📊 Group ${selectedGroup.name} has ${categories.size} categories that will be deleted")
            } else {
                context.categoriesCountToDelete = 0
            }
        }

        then {
            to = MoneyManagerState.GROUP_DELETE_CONFIRM
        }
    }

    // Отмена выбора группы
    transition {
        name = "Cancel delete selection"

        condition {
            from = MoneyManagerState.GROUP_DELETE_SELECT
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }

    // Подтверждение удаления
    transition {
        name = "Confirm delete group"

        condition {
            from = MoneyManagerState.GROUP_DELETE_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_DELETE
        }

        action {
            val userId = user.id
            val groupId = context.currentGroup?.id ?: return@action

            val deleted = groupService.deleteGroup(userId, groupId)

            if (deleted) {
                // Обновляем список групп
                val groups = groupService.getUserGroups(userId)
                context.userGroups = groups

                // Обновляем активную группу
                val activeGroup = groupService.getActiveGroup(userId)
                context.userInfo = context.userInfo?.copy(
                    activeGroupId = activeGroup?.id,
                    groupIds = groups.mapNotNull { it.id }.toSet()
                )

                context.currentGroup = null
            }
        }

        then {
            to = MoneyManagerState.GROUP_LIST
        }
    }

    // Отмена удаления
    transition {
        name = "Cancel delete group"

        condition {
            from = MoneyManagerState.GROUP_DELETE_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        then {
            to = MoneyManagerState.GROUP_DELETE_SELECT
        }
    }
}

/**
 * Возврат в главное меню
 */
private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.backToMenuTransition() {
    // Возврат из управления группами
    transition {
        name = "Back to menu from group management"

        condition {
            from = MoneyManagerState.GROUP_MANAGEMENT
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    // Возврат из списка групп
    transition {
        name = "Back to group management from group list"

        condition {
            from = MoneyManagerState.GROUP_LIST
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }

    // Возврат из показа инвайта
    transition {
        name = "Back to group management from group invite"

        condition {
            from = MoneyManagerState.GROUP_INVITE_SHOW
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }

    // Возврат из списка участников
    transition {
        name = "Back to group management from group members"

        condition {
            from = MoneyManagerState.GROUP_MEMBERS
            button = MoneyManagerButtonType.BACK_TO_MENU
        }

        then {
            to = MoneyManagerState.GROUP_MANAGEMENT
        }
    }
}