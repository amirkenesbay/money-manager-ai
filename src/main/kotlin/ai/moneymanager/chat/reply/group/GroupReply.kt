package ai.moneymanager.chat.reply.group

import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupManagementReply() {
    reply {
        state = MoneyManagerState.GROUP_MANAGEMENT

        message {
            text = """
                👥 Управление группами

                Здесь вы можете создать новую группу для совместного учета или присоединиться к существующей.
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "➕ Создать группу"
                        type = MoneyManagerButtonType.CREATE_GROUP
                    }
                }
                buttonRow {
                    button {
                        text = "📋 Мои группы"
                        type = MoneyManagerButtonType.MY_GROUPS
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад в меню"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupCreateEnterNameReply() {
    reply {
        state = MoneyManagerState.GROUP_CREATE_ENTER_NAME

        message {
            text = """
                ➕ Создание новой группы

                Введите название группы или выберите готовый вариант:
            """.trimIndent()

            keyboard {
                // Быстрые варианты названий
                buttonRow {
                    button {
                        text = "👨‍👩‍👧‍👦 Семья"
                        type = MoneyManagerButtonType.QUICK_GROUP_FAMILY
                    }
                    button {
                        text = "👥 С другом"
                        type = MoneyManagerButtonType.QUICK_GROUP_WITH_FRIEND
                    }
                }
                buttonRow {
                    button {
                        text = "💼 Работа"
                        type = MoneyManagerButtonType.QUICK_GROUP_WORK
                    }
                    button {
                        text = "✈️ Поездка"
                        type = MoneyManagerButtonType.QUICK_GROUP_TRIP
                    }
                }

                // Кнопка отмены
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupInviteShowReply() {
    reply {
        state = MoneyManagerState.GROUP_INVITE_SHOW

        message {
            // Если быстрое создание (кнопка) - редактируем сообщение, если ручной ввод - новое сообщение
            newMessage = !context.isQuickGroupCreation

            val group = context.currentGroup
            if (group != null) {
                val botUsername = "moneyManagerAIbot"
                text = """
                    ✅ Группа "${group.name}" создана!

                    Пригласите участников, отправив им эту ссылку:
                    https://t.me/$botUsername?start=join_${group.inviteToken}

                    Или они могут отправить код: ${group.inviteToken}
                """.trimIndent()
            } else {
                text = "Ошибка при создании группы"
            }

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад в меню"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupJoinConfirmReply() {
    reply {
        state = MoneyManagerState.GROUP_JOIN_CONFIRM

        message {
            val group = context.pendingGroup

            if (group != null) {
                text = """
                    👥 Приглашение в группу "${group.name}"

                    Участников: ${group.memberIds.size}
                    Создатель: ID ${group.ownerId}

                    Присоединиться к этой группе?
                """.trimIndent()

                keyboard {
                    buttonRow {
                        button {
                            text = "✅ Присоединиться"
                            type = MoneyManagerButtonType.CONFIRM_JOIN
                        }
                    }
                    buttonRow {
                        button {
                            text = "❌ Отклонить"
                            type = MoneyManagerButtonType.CANCEL
                        }
                    }
                }
            } else {
                text = "Группа не найдена или приглашение недействительно"

                keyboard {
                    buttonRow {
                        button {
                            text = "⬅️ Назад в меню"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupListReply() {
    reply {
        state = MoneyManagerState.GROUP_LIST

        message {
            val userInfo = context.userInfo
            val userGroups = context.userGroups

            if (userGroups.isNotEmpty()) {
                // Формируем список групп с номерами
                val groupsList = userGroups.mapIndexed { index, group ->
                    val ownerMark = if (group.ownerId == userInfo?.telegramUserId) " 👑" else ""
                    "${index + 1} - ${group.name}$ownerMark"
                }.joinToString("\n")

                // Находим активную группу
                val activeGroup = userGroups.find { it.id == userInfo?.activeGroupId }
                val activeGroupText = if (activeGroup != null) {
                    val activeIndex = userGroups.indexOf(activeGroup) + 1
                    "$activeIndex - ${activeGroup.name}"
                } else {
                    "Не выбрана"
                }

                text = """
                    |📋 Мои группы
                    |
                    |У вас ${userGroups.size} групп(ы)
                    |
                    |$groupsList
                    |
                    |Активная группа: $activeGroupText
                    |
                    |Выберите номер группы для переключения:
                    |👑 - вы владелец группы
                """.trimMargin()

                keyboard {
                    // Создаем кнопки для каждой группы (по 3 в ряд)
                    userGroups.chunked(3).forEach { groupsInRow ->
                        buttonRow {
                            groupsInRow.forEachIndexed { _, group ->
                                button {
                                    val index = userGroups.indexOf(group) + 1
                                    text = if (group.id == userInfo?.activeGroupId) {
                                        "✅ $index"
                                    } else {
                                        "$index"
                                    }
                                    type = MoneyManagerButtonType.SWITCH_GROUP
                                }
                            }
                        }
                    }

                    // Кнопки "Редактировать", "Удалить" и "Назад"
                    buttonRow {
                        button {
                            text = "✏️ Редактировать группу"
                            type = MoneyManagerButtonType.EDIT_GROUP
                        }
                    }
                    buttonRow {
                        button {
                            text = "🗑 Удалить группу"
                            type = MoneyManagerButtonType.DELETE_GROUP
                        }
                    }
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            } else {
                text = """
                    📋 Мои группы

                    У вас пока нет групп. Создайте новую группу для совместного учета!
                """.trimIndent()

                keyboard {
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupDeleteSelectReply() {
    reply {
        state = MoneyManagerState.GROUP_DELETE_SELECT

        message {
            val userInfo = context.userInfo
            val userGroups = context.userGroups

            // Фильтруем только те группы, где пользователь является владельцем
            val ownedGroups = userGroups.filter { it.ownerId == userInfo?.telegramUserId }

            if (ownedGroups.isNotEmpty()) {
                val groupsList = ownedGroups.mapIndexed { index, group ->
                    "${index + 1} - ${group.name}"
                }.joinToString("\n")

                text = """
                    |🗑 Удаление группы
                    |
                    |Выберите группу для удаления:
                    |
                    |$groupsList
                    |
                    |⚠️ Вы можете удалять только те группы, где вы являетесь владельцем.
                """.trimMargin()

                keyboard {
                    // Создаем кнопки только для групп, где пользователь - владелец
                    ownedGroups.chunked(3).forEach { groupsInRow ->
                        buttonRow {
                            groupsInRow.forEach { group ->
                                button {
                                    val index = ownedGroups.indexOf(group) + 1
                                    text = "$index"
                                    type = MoneyManagerButtonType.DELETE_GROUP
                                }
                            }
                        }
                    }

                    buttonRow {
                        button {
                            text = "❌ Отмена"
                            type = MoneyManagerButtonType.CANCEL
                        }
                    }
                }
            } else {
                text = """
                    ❌ Нет групп для удаления

                    У вас нет групп, где вы являетесь владельцем.
                """.trimIndent()

                keyboard {
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupDeleteConfirmReply() {
    reply {
        state = MoneyManagerState.GROUP_DELETE_CONFIRM

        message {
            val group = context.currentGroup
            val userInfo = context.userInfo

            if (group != null) {
                val isOwner = group.ownerId == userInfo?.telegramUserId

                if (isOwner) {
                    text = """
                        |⚠️ Удаление группы "${group.name}"
                        |
                        |Вы уверены, что хотите удалить эту группу?
                        |
                        |⚠️ Это действие нельзя отменить!
                        |Все участники потеряют доступ к группе.
                    """.trimMargin()

                    keyboard {
                        buttonRow {
                            button {
                                text = "✅ Да, удалить"
                                type = MoneyManagerButtonType.CONFIRM_DELETE
                            }
                        }
                        buttonRow {
                            button {
                                text = "❌ Отмена"
                                type = MoneyManagerButtonType.CANCEL
                            }
                        }
                    }
                } else {
                    text = """
                        ❌ Ошибка

                        Только владелец группы может её удалить.
                    """.trimIndent()

                    keyboard {
                        buttonRow {
                            button {
                                text = "⬅️ Назад"
                                type = MoneyManagerButtonType.BACK_TO_MENU
                            }
                        }
                    }
                }
            } else {
                text = "Группа не найдена"

                keyboard {
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupEditSelectReply() {
    reply {
        state = MoneyManagerState.GROUP_EDIT_SELECT

        message {
            val userInfo = context.userInfo
            val userGroups = context.userGroups

            // Фильтруем только те группы, где пользователь является владельцем
            val ownedGroups = userGroups.filter { it.ownerId == userInfo?.telegramUserId }

            if (ownedGroups.isNotEmpty()) {
                val groupsList = ownedGroups.mapIndexed { index, group ->
                    "${index + 1} - ${group.name}"
                }.joinToString("\n")

                text = """
                    |✏️ Редактирование группы
                    |
                    |Выберите группу для редактирования:
                    |
                    |$groupsList
                    |
                    |⚠️ Вы можете редактировать только те группы, где вы являетесь владельцем.
                """.trimMargin()

                keyboard {
                    // Создаем кнопки только для групп, где пользователь - владелец
                    ownedGroups.chunked(3).forEach { groupsInRow ->
                        buttonRow {
                            groupsInRow.forEach { group ->
                                button {
                                    val index = ownedGroups.indexOf(group) + 1
                                    text = "$index"
                                    type = MoneyManagerButtonType.EDIT_GROUP
                                }
                            }
                        }
                    }

                    buttonRow {
                        button {
                            text = "❌ Отмена"
                            type = MoneyManagerButtonType.CANCEL
                        }
                    }
                }
            } else {
                text = """
                    ❌ Нет групп для редактирования

                    У вас нет групп, где вы являетесь владельцем.
                """.trimIndent()

                keyboard {
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupEditEnterNameReply() {
    reply {
        state = MoneyManagerState.GROUP_EDIT_ENTER_NAME

        message {
            val group = context.currentGroup

            if (group != null) {
                text = """
                    |✏️ Редактирование группы "${group.name}"
                    |
                    |Введите новое название группы:
                """.trimMargin()

                keyboard {
                    buttonRow {
                        button {
                            text = "❌ Отмена"
                            type = MoneyManagerButtonType.CANCEL
                        }
                    }
                }
            } else {
                text = "Группа не найдена"

                keyboard {
                    buttonRow {
                        button {
                            text = "⬅️ Назад"
                            type = MoneyManagerButtonType.BACK_TO_MENU
                        }
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupMembersReply() {
    reply {
        state = MoneyManagerState.GROUP_MEMBERS

        message {
            val group = context.currentGroup
            if (group != null) {
                text = """
                    👥 Участники группы "${group.name}"

                    Всего участников: ${group.memberIds.size}
                    Создатель: ID ${group.ownerId}

                    ${group.memberIds.joinToString("\n") { "• Пользователь ID: $it" }}
                """.trimIndent()
            } else {
                text = "Группа не найдена"
            }

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}