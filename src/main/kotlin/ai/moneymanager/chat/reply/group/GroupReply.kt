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
                    |🔗 Ссылка для приглашения в группу "${group.name}"
                    |
                    |Отправьте участникам эту ссылку:
                    |https://t.me/$botUsername?start=join_${group.inviteToken}
                """.trimMargin()
            } else {
                text = "Ошибка: группа не найдена"
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


fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupJoinConfirmReply() {
    reply {
        state = MoneyManagerState.GROUP_JOIN_CONFIRM

        message {
            val group = context.pendingGroup
            val ownerInfo = context.pendingGroupOwnerInfo

            if (group != null) {
                // Формируем имя создателя
                val ownerName = when {
                    ownerInfo != null -> {
                        val firstName = ownerInfo.firstName ?: ""
                        val lastName = ownerInfo.lastName ?: ""
                        val fullName = "$firstName $lastName".trim()

                        if (fullName.isNotEmpty()) {
                            fullName
                        } else if (!ownerInfo.username.isNullOrEmpty()) {
                            "@${ownerInfo.username}"
                        } else {
                            "ID ${group.ownerId}"
                        }
                    }
                    else -> "ID ${group.ownerId}"
                }

                text = """
                    |👥 Приглашение в группу "${group.name}"
                    |
                    |Участников: ${group.memberIds.size}
                    |Создатель: $ownerName
                    |
                    |Присоединиться к этой группе?
                """.trimMargin()

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
                text = """
                    |📋 Мои группы
                    |
                    |Выберите группу:
                    |👑 - владелец, ✅ - активная
                """.trimMargin()

                keyboard {
                    userGroups.forEach { group ->
                        buttonRow {
                            button {
                                val isOwner = group.ownerId == userInfo?.telegramUserId
                                val isActive = group.id == userInfo?.activeGroupId
                                val prefix = when {
                                    isActive && isOwner -> "✅ 👑 "
                                    isActive -> "✅ "
                                    isOwner -> "👑 "
                                    else -> ""
                                }
                                text = "$prefix${group.name}"
                                type = MoneyManagerButtonType.GROUP_ITEM
                            }
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


fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupActionsReply() {
    reply {
        state = MoneyManagerState.GROUP_ACTIONS

        message {
            val group = context.currentGroup
            val userInfo = context.userInfo
            val isOwner = group?.ownerId == userInfo?.telegramUserId
            val isActive = group?.id == userInfo?.activeGroupId

            if (group != null) {
                val ownerText = if (isOwner) "👑 Вы владелец" else ""
                val activeText = if (isActive) "✅ Активная группа" else ""
                val statusLine = listOf(ownerText, activeText).filter { it.isNotEmpty() }.joinToString(" · ")

                text = """
                    |👥 Группа "${group.name}"
                    |
                    |${if (statusLine.isNotEmpty()) "$statusLine\n|" else ""}
                    |Участников: ${group.memberIds.size}
                """.trimMargin()

                keyboard {
                    if (isOwner) {
                        buttonRow {
                            button {
                                text = "✏️ Изменить название"
                                type = MoneyManagerButtonType.EDIT_GROUP
                            }
                        }
                        buttonRow {
                            button {
                                text = "🔗 Пригласить в группу"
                                type = MoneyManagerButtonType.INVITE_TO_GROUP
                            }
                        }
                    }
                    buttonRow {
                        button {
                            text = "👥 Участники"
                            type = MoneyManagerButtonType.GROUP_MEMBERS
                        }
                    }
                    if (!isActive) {
                        buttonRow {
                            button {
                                text = "✅ Сделать активной"
                                type = MoneyManagerButtonType.SWITCH_GROUP
                            }
                        }
                    }
                    if (isOwner) {
                        buttonRow {
                            button {
                                text = "🗑 Удалить группу"
                                type = MoneyManagerButtonType.DELETE_GROUP
                            }
                        }
                    }
                    buttonRow {
                        button {
                            text = "⬅️ Назад к списку"
                            type = MoneyManagerButtonType.BACK_TO_MENU
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupDeleteConfirmReply() {
    reply {
        state = MoneyManagerState.GROUP_DELETE_CONFIRM

        message {
            val group = context.currentGroup
            val userInfo = context.userInfo
            val categoriesCount = context.categoriesCountToDelete

            if (group != null) {
                val isOwner = group.ownerId == userInfo?.telegramUserId

                if (isOwner) {
                    val categoriesWarning = if (categoriesCount > 0) {
                        "\n🗑 Будет удалено категорий: $categoriesCount\n"
                    } else {
                        "\n📋 В группе нет категорий.\n"
                    }

                    text = """
                        |⚠️ Удаление группы "${group.name}"
                        |
                        |Вы уверены, что хотите удалить эту группу?
                        |$categoriesWarning
                        |⚠️ Это действие нельзя отменить!
                        |Все участники потеряют доступ к группе.
                        |Все категории группы будут удалены безвозвратно.
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