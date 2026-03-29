package ai.moneymanager.chat.reply.group

import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.cancelButton
import ai.moneymanager.chat.reply.common.confirmAndCancelButtons
import ai.moneymanager.chat.reply.common.formatUserDisplayName
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupManagementReply() {
    reply {
        state = MoneyManagerState.GROUP_MANAGEMENT

        message {
            text = """
                |👥 Совместный учёт
                |
                |Ведите финансы вместе с семьёй, друзьями или коллегами. Создайте группу и пригласите участников по ссылке.
            """.trimMargin()

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
                backButton("⬅️ Назад в меню")
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupCreateEnterNameReply() {
    reply {
        state = MoneyManagerState.GROUP_CREATE_ENTER_NAME

        message {
            // show-once: read and reset
            val errorText = if (context.groupNameDuplicateError) {
                "\n\n⚠️ Группа с названием «${context.groupNameInput}» уже существует. Введите другое название:"
            } else ""
            context.groupNameDuplicateError = false

            text = """
                |➕ Новая группа для совместного учёта
                |
                |Введите название или выберите готовый вариант:$errorText
            """.trimMargin()

            keyboard {
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
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupInviteShowReply() {
    reply {
        state = MoneyManagerState.GROUP_INVITE_SHOW

        message {
            newMessage = !context.isQuickGroupCreation && !context.inviteFromActions

            val group = context.currentGroup
            if (group != null) {
                val botUsername = "moneyManagerAIbot"
                val activeNotice = if (!context.inviteFromActions) {
                    "\n|\n|✅ Группа установлена как активная"
                } else ""

                text = """
                    |🔗 Пригласите участников в "${group.name}"
                    |
                    |Отправьте эту ссылку тем, с кем хотите вести совместный учёт:
                    |https://t.me/$botUsername?start=join_${group.inviteToken}$activeNotice
                """.trimMargin()
            } else {
                text = "Ошибка: группа не найдена"
            }

            keyboard {
                backButton()
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
                val ownerName = formatUserDisplayName(ownerInfo, group.ownerId)

                text = """
                    |👥 Приглашение в совместный учёт "${group.name}"
                    |
                    |Участников: ${group.memberIds.size}
                    |Создатель: $ownerName
                    |
                    |Присоединиться?
                """.trimMargin()

                keyboard {
                    confirmAndCancelButtons(
                        confirmText = "✅ Присоединиться",
                        confirmType = MoneyManagerButtonType.CONFIRM_JOIN,
                        cancelText = "❌ Отклонить"
                    )
                }
            } else {
                text = "Группа не найдена или приглашение недействительно"

                keyboard {
                    backButton("⬅️ Назад в меню")
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
                    backButton()
                }
            } else {
                text = """
                    |📋 Мои группы
                    |
                    |У вас пока нет групп. Создайте группу, чтобы вести учёт вместе с другими!
                """.trimMargin()

                keyboard {
                    backButton()
                }
            }
        }
    }
}


fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupActionsReply() {
    reply {
        state = MoneyManagerState.GROUP_ACTIONS

        message {
            // show-once: read and reset
            if (context.textInputResponse) {
                newPinnedMessage = true
                context.textInputResponse = false
            }

            val group = context.currentGroup
            val userInfo = context.userInfo
            val isOwner = group?.ownerId == userInfo?.telegramUserId
            val isActive = group?.id == userInfo?.activeGroupId

            if (group != null) {
                val isPersonal = group.type == GroupType.PERSONAL
                val ownerText = if (isOwner) "👑 Вы владелец" else ""
                val activeText = if (isActive) "✅ Активная группа" else ""
                val statusLine = listOf(ownerText, activeText).filter { it.isNotEmpty() }.joinToString(" · ")

                // show-once: read and reset
                val confirmation = context.renameConfirmation?.let { "\n|\n|$it" } ?: ""
                context.renameConfirmation = null

                val personalHint = if (isOwner && isPersonal) {
                    "\n|\n|💡 Для совместного учёта создайте новую группу в разделе «Совместный учёт»"
                } else ""

                text = """
                    |👥 Группа "${group.name}"
                    |
                    |${if (statusLine.isNotEmpty()) "$statusLine\n|" else ""}
                    |Участников: ${group.memberIds.size}$confirmation$personalHint
                """.trimMargin()

                keyboard {
                    if (isOwner) {
                        buttonRow {
                            button {
                                text = "✏️ Изменить название"
                                type = MoneyManagerButtonType.EDIT_GROUP
                            }
                        }
                        if (!isPersonal) {
                            buttonRow {
                                button {
                                    text = "🔗 Пригласить в группу"
                                    type = MoneyManagerButtonType.INVITE_TO_GROUP
                                }
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
                    if (isOwner && !isPersonal) {
                        buttonRow {
                            button {
                                text = "🗑 Удалить группу"
                                type = MoneyManagerButtonType.DELETE_GROUP
                            }
                        }
                    }
                    backButton("⬅️ Назад к списку")
                }
            } else {
                text = "Группа не найдена"

                keyboard {
                    backButton()
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
                        confirmAndCancelButtons()
                    }
                } else {
                    text = """
                        ❌ Ошибка

                        Только владелец группы может её удалить.
                    """.trimIndent()

                    keyboard {
                        backButton()
                    }
                }
            } else {
                text = "Группа не найдена"

                keyboard {
                    backButton()
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
                    cancelButton()
                }
            } else {
                text = "Группа не найдена"

                keyboard {
                    backButton()
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
            val membersList = context.groupMembersList

            if (group != null) {
                val ownerName = formatUserDisplayName(
                    membersList.find { it.telegramUserId == group.ownerId },
                    group.ownerId
                )

                val membersText = if (membersList.isNotEmpty()) {
                    membersList.joinToString("\n") { member ->
                        val isOwner = member.telegramUserId == group.ownerId
                        val memberName = formatUserDisplayName(member, member.telegramUserId ?: 0)
                        if (isOwner) "• 👑 $memberName" else "• $memberName"
                    }
                } else {
                    group.memberIds.joinToString("\n") { "• Пользователь ID: $it" }
                }

                text = """
                    |👥 Участники группы "${group.name}"
                    |
                    |Всего участников: ${group.memberIds.size}
                    |Создатель: $ownerName
                    |
                    |$membersText
                """.trimMargin()
            } else {
                text = "Группа не найдена"
            }

            keyboard {
                backButton()
            }
        }
    }
}
