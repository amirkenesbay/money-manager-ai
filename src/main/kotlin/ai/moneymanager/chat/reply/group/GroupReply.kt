package ai.moneymanager.chat.reply.group

import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.cancelButton
import ai.moneymanager.chat.reply.common.confirmAndCancelButtons
import ai.moneymanager.chat.reply.common.formatUserDisplayName
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupManagementReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_MANAGEMENT

        message {
            val lang = context.userInfo?.language
            val createText = localizationService.t("group.button.create", lang)
            val myGroupsText = localizationService.t("group.button.my_groups", lang)
            val backText = localizationService.t("common.back_to_menu", lang)

            text = localizationService.t("group.management.text", lang)

            keyboard {
                buttonRow {
                    button {
                        text = createText
                        type = MoneyManagerButtonType.CREATE_GROUP
                    }
                }
                buttonRow {
                    button {
                        text = myGroupsText
                        type = MoneyManagerButtonType.MY_GROUPS
                    }
                }
                backButton(backText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupCreateEnterNameReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_CREATE_ENTER_NAME

        message {
            val lang = context.userInfo?.language
            val errorText = if (context.groupNameDuplicateError) {
                localizationService.t("group.create.error.duplicate", lang, context.groupNameInput ?: "")
            } else ""
            context.groupNameDuplicateError = false

            val title = localizationService.t("group.create.title", lang)
            val cancelText = localizationService.t("common.cancel", lang)

            if (context.customNameInputMode) {
                val prompt = localizationService.t("group.create.prompt.custom", lang)
                text = "$title\n\n$prompt$errorText"

                keyboard {
                    cancelButton(cancelText)
                }
            } else {
                val prompt = localizationService.t("group.create.prompt.quick", lang)
                val familyText = localizationService.t("group.create.button.quick.family", lang)
                val friendText = localizationService.t("group.create.button.quick.with_friend", lang)
                val workText = localizationService.t("group.create.button.quick.work", lang)
                val tripText = localizationService.t("group.create.button.quick.trip", lang)
                val enterCustomText = localizationService.t("group.create.button.enter_custom", lang)

                text = "$title\n\n$prompt$errorText"

                keyboard {
                    buttonRow {
                        button {
                            text = familyText
                            type = MoneyManagerButtonType.QUICK_GROUP_FAMILY
                        }
                        button {
                            text = friendText
                            type = MoneyManagerButtonType.QUICK_GROUP_WITH_FRIEND
                        }
                    }
                    buttonRow {
                        button {
                            text = workText
                            type = MoneyManagerButtonType.QUICK_GROUP_WORK
                        }
                        button {
                            text = tripText
                            type = MoneyManagerButtonType.QUICK_GROUP_TRIP
                        }
                    }
                    buttonRow {
                        button {
                            text = enterCustomText
                            type = MoneyManagerButtonType.ENTER_CUSTOM_NAME
                        }
                    }
                    cancelButton(cancelText)
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupInviteShowReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_INVITE_SHOW

        message {
            newMessage = !context.isQuickGroupCreation && !context.inviteFromActions

            val lang = context.userInfo?.language
            val group = context.currentGroup
            val backText = localizationService.t("common.back", lang)

            if (group != null) {
                val botUsername = "moneyManagerAIbot"
                val title = localizationService.t("group.invite.title", lang, group.name)
                val description = localizationService.t("group.invite.description", lang)
                val activeNotice = if (!context.inviteFromActions) {
                    localizationService.t("group.invite.active_notice", lang)
                } else ""

                text = """
                    |$title
                    |
                    |$description
                    |https://t.me/$botUsername?start=join_${group.inviteToken}$activeNotice
                """.trimMargin()
            } else {
                text = localizationService.t("group.error.not_found", lang)
            }

            keyboard {
                backButton(backText)
            }
        }
    }
}


fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupJoinConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_JOIN_CONFIRM

        message {
            val lang = context.userInfo?.language
            val group = context.pendingGroup
            val ownerInfo = context.pendingGroupOwnerInfo

            if (group != null) {
                val ownerName = formatUserDisplayName(ownerInfo, group.ownerId)
                val title = localizationService.t("group.join.title", lang, group.name)
                val membersLine = localizationService.t("group.join.members", lang, group.memberIds.size)
                val creatorLine = localizationService.t("group.join.creator", lang, ownerName)
                val prompt = localizationService.t("group.join.prompt", lang)
                val confirmText = localizationService.t("group.join.button.confirm", lang)
                val cancelText = localizationService.t("group.join.button.cancel", lang)

                text = """
                    |$title
                    |
                    |$membersLine
                    |$creatorLine
                    |
                    |$prompt
                """.trimMargin()

                keyboard {
                    confirmAndCancelButtons(
                        confirmText = confirmText,
                        confirmType = MoneyManagerButtonType.CONFIRM_JOIN,
                        cancelText = cancelText
                    )
                }
            } else {
                text = localizationService.t("group.join.invite_invalid", lang)

                keyboard {
                    backButton(localizationService.t("common.back_to_menu", lang))
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupListReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_LIST

        message {
            val lang = context.userInfo?.language
            val userInfo = context.userInfo
            val userGroups = context.userGroups
            val backText = localizationService.t("common.back", lang)

            if (userGroups.isNotEmpty()) {
                val title = localizationService.t("group.list.title", lang)
                val subtitle = localizationService.t("group.list.subtitle", lang)
                val legend = localizationService.t("group.list.legend", lang)

                text = """
                    |$title
                    |
                    |$subtitle
                    |$legend
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
                    backButton(backText)
                }
            } else {
                text = localizationService.t("group.list.empty", lang)

                keyboard {
                    backButton(backText)
                }
            }
        }
    }
}


fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupActionsReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_ACTIONS

        message {
            val lang = context.userInfo?.language

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
                val ownerText = if (isOwner) localizationService.t("group.actions.owner", lang) else ""
                val activeText = if (isActive) localizationService.t("group.actions.active", lang) else ""
                val statusLine = listOf(ownerText, activeText).filter { it.isNotEmpty() }.joinToString(" · ")

                val confirmation = context.renameConfirmation?.let { "\n\n$it" } ?: ""
                context.renameConfirmation = null

                val personalHint = if (isOwner && isPersonal) {
                    "\n\n" + localizationService.t("group.actions.personal_hint", lang)
                } else ""

                val title = localizationService.t("group.actions.title", lang, group.name)
                val membersLine = localizationService.t("group.actions.members_count", lang, group.memberIds.size)
                val statusBlock = if (statusLine.isNotEmpty()) "$statusLine\n" else ""

                text = """
                    |$title
                    |
                    |$statusBlock$membersLine$confirmation$personalHint
                """.trimMargin()

                val editNameText = localizationService.t("group.actions.button.edit_name", lang)
                val inviteText = localizationService.t("group.actions.button.invite", lang)
                val membersText = localizationService.t("group.actions.button.members", lang)
                val makeActiveText = localizationService.t("group.actions.button.make_active", lang)
                val deleteText = localizationService.t("group.actions.button.delete", lang)
                val backText = localizationService.t("common.back_to_list", lang)

                keyboard {
                    if (isOwner) {
                        buttonRow {
                            button {
                                text = editNameText
                                type = MoneyManagerButtonType.EDIT_GROUP
                            }
                        }
                        if (!isPersonal) {
                            buttonRow {
                                button {
                                    text = inviteText
                                    type = MoneyManagerButtonType.INVITE_TO_GROUP
                                }
                            }
                        }
                    }
                    buttonRow {
                        button {
                            text = membersText
                            type = MoneyManagerButtonType.GROUP_MEMBERS
                        }
                    }
                    if (!isActive) {
                        buttonRow {
                            button {
                                text = makeActiveText
                                type = MoneyManagerButtonType.SWITCH_GROUP
                            }
                        }
                    }
                    if (isOwner && !isPersonal) {
                        buttonRow {
                            button {
                                text = deleteText
                                type = MoneyManagerButtonType.DELETE_GROUP
                            }
                        }
                    }
                    backButton(backText)
                }
            } else {
                text = localizationService.t("group.error.not_found", lang)

                keyboard {
                    backButton(localizationService.t("common.back", lang))
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupDeleteConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_DELETE_CONFIRM

        message {
            val lang = context.userInfo?.language
            val group = context.currentGroup
            val userInfo = context.userInfo
            val categoriesCount = context.categoriesCountToDelete
            val backText = localizationService.t("common.back", lang)

            if (group != null) {
                val isOwner = group.ownerId == userInfo?.telegramUserId

                if (isOwner) {
                    val categoriesWarning = if (categoriesCount > 0) {
                        "\n" + localizationService.t("group.delete.categories_warning", lang, categoriesCount) + "\n"
                    } else {
                        "\n" + localizationService.t("group.delete.no_categories", lang) + "\n"
                    }

                    val title = localizationService.t("group.delete.title", lang, group.name)
                    val prompt = localizationService.t("group.delete.prompt", lang)
                    val finalWarning = localizationService.t("group.delete.final_warning", lang)
                    val confirmText = localizationService.t("common.confirm.delete_yes", lang)
                    val cancelText = localizationService.t("common.cancel", lang)

                    text = """
                        |$title
                        |
                        |$prompt
                        |$categoriesWarning
                        |$finalWarning
                    """.trimMargin()

                    keyboard {
                        confirmAndCancelButtons(
                            confirmText = confirmText,
                            cancelText = cancelText
                        )
                    }
                } else {
                    text = localizationService.t("group.delete.not_owner", lang)

                    keyboard {
                        backButton(backText)
                    }
                }
            } else {
                text = localizationService.t("group.error.not_found", lang)

                keyboard {
                    backButton(backText)
                }
            }
        }
    }
}


fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupEditEnterNameReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_EDIT_ENTER_NAME

        message {
            val lang = context.userInfo?.language
            val group = context.currentGroup

            if (group != null) {
                val title = localizationService.t("group.edit.title", lang, group.name)
                val prompt = localizationService.t("group.edit.prompt", lang)
                val cancelText = localizationService.t("common.cancel", lang)

                text = """
                    |$title
                    |
                    |$prompt
                """.trimMargin()

                keyboard {
                    cancelButton(cancelText)
                }
            } else {
                text = localizationService.t("group.error.not_found", lang)

                keyboard {
                    backButton(localizationService.t("common.back", lang))
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.groupMembersReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.GROUP_MEMBERS

        message {
            val lang = context.userInfo?.language
            val group = context.currentGroup
            val membersList = context.groupMembersList
            val backText = localizationService.t("common.back", lang)

            if (group != null) {
                val ownerName = formatUserDisplayName(
                    membersList.find { it.telegramUserId == group.ownerId },
                    group.ownerId
                )

                val unknownTemplate = "group.members.unknown"
                val membersText = if (membersList.isNotEmpty()) {
                    membersList.joinToString("\n") { member ->
                        val isOwner = member.telegramUserId == group.ownerId
                        val memberName = formatUserDisplayName(member, member.telegramUserId ?: 0)
                        if (isOwner) "• 👑 $memberName" else "• $memberName"
                    }
                } else {
                    group.memberIds.joinToString("\n") { localizationService.t(unknownTemplate, lang, it) }
                }

                val title = localizationService.t("group.members.title", lang, group.name)
                val totalLine = localizationService.t("group.members.total", lang, group.memberIds.size)
                val creatorLine = localizationService.t("group.members.creator", lang, ownerName)

                text = """
                    |$title
                    |
                    |$totalLine
                    |$creatorLine
                    |
                    |$membersText
                """.trimMargin()
            } else {
                text = localizationService.t("group.error.not_found", lang)
            }

            keyboard {
                backButton(backText)
            }
        }
    }
}
