package ai.moneymanager.chat.reply.category

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.cancelButton
import ai.moneymanager.chat.reply.common.confirmAndCancelButtons
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.QuickCategoryTemplate
import ai.moneymanager.domain.model.QuickTemplates
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

private const val CATEGORY_LIST_TITLE_FALLBACK = "📋"

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryManagementReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_MANAGEMENT

        message {
            val lang = context.userInfo?.language
            val title = localizationService.t("category.management.title", lang)
            val subtitle = localizationService.t("category.management.subtitle", lang)
            val groupLine = context.activeGroupName
                ?.let { "\n" + localizationService.t("category.management.group_line", lang, it) }
                ?: ""
            val createText = localizationService.t("category.button.create", lang)
            val myCategoriesText = localizationService.t("category.button.my_categories", lang)
            val backText = localizationService.t("common.back_to_menu", lang)

            text = """
                |$title$groupLine
                |
                |$subtitle
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = createText
                        type = MoneyManagerButtonType.CREATE_CATEGORY
                    }
                }
                buttonRow {
                    button {
                        text = myCategoriesText
                        type = MoneyManagerButtonType.MY_CATEGORIES
                    }
                }
                backButton(backText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryNoGroupWarningReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_NO_GROUP_WARNING

        message {
            val lang = context.userInfo?.language
            val createGroupText = localizationService.t("group.button.create", lang)
            val backText = localizationService.t("common.back", lang)

            text = localizationService.t("category.no_group.text", lang)

            keyboard {
                buttonRow {
                    button {
                        text = createGroupText
                        type = MoneyManagerButtonType.CREATE_GROUP
                    }
                }
                backButton(backText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateSelectTypeReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE

        message {
            val lang = context.userInfo?.language
            val expenseText = localizationService.t("category.button.type.expense_singular", lang)
            val incomeText = localizationService.t("category.button.type.income_singular", lang)
            val cancelText = localizationService.t("common.cancel", lang)

            text = localizationService.t("category.create.select_type.text", lang)

            keyboard {
                buttonRow {
                    button {
                        text = expenseText
                        type = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
                    }
                }
                buttonRow {
                    button {
                        text = incomeText
                        type = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
                    }
                }
                cancelButton(cancelText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateEnterNameReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME

        message {
            val lang = context.userInfo?.language
            val categoryType = context.categoryTypeInput
            val titleKey = createTitleKey(categoryType)
            val title = localizationService.t(titleKey, lang)
            val cancelText = localizationService.t("common.cancel", lang)

            if (context.customNameInputMode) {
                val prompt = localizationService.t("category.create.prompt.custom", lang)
                text = "$title\n\n$prompt"

                keyboard {
                    cancelButton(cancelText)
                }
            } else {
                val prompt = localizationService.t("category.create.prompt.quick", lang)
                val enterCustomText = localizationService.t("category.create.button.enter_custom", lang)
                val templates = quickTemplatesFor(categoryType)

                text = "$title\n\n$prompt"

                keyboard {
                    templates.chunked(2).forEach { row ->
                        buttonRow {
                            row.forEach { template ->
                                button {
                                    text = formatQuickButton(template, lang, localizationService)
                                    type = template.buttonType
                                }
                            }
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

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateResultReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_RESULT

        message {
            newMessage = !context.isQuickCategoryCreation

            val lang = context.userInfo?.language
            val category = context.currentCategory
            val backText = localizationService.t("common.back", lang)

            text = if (category != null) {
                val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                val key = createSuccessKey(category.type)
                localizationService.t(key, lang, icon, category.name)
            } else {
                val name = context.categoryNameInput ?: ""
                localizationService.t("category.create.result.duplicate", lang, name)
            }

            keyboard {
                if (category != null) {
                    val createMoreText = localizationService.t("category.create.result.button.create_more", lang)
                    buttonRow {
                        button {
                            text = createMoreText
                            type = MoneyManagerButtonType.CREATE_CATEGORY
                        }
                    }
                }
                backButton(backText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryListSelectTypeReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE

        message {
            val lang = context.userInfo?.language
            val expenseText = localizationService.t("category.button.type.expense_plural", lang)
            val incomeText = localizationService.t("category.button.type.income_plural", lang)
            val deleteAllText = localizationService.t("category.list.button.delete_all", lang)
            val backText = localizationService.t("common.back", lang)

            text = localizationService.t("category.list.select_type.text", lang)

            keyboard {
                buttonRow {
                    button {
                        text = expenseText
                        type = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
                    }
                }
                buttonRow {
                    button {
                        text = incomeText
                        type = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = deleteAllText
                        type = MoneyManagerButtonType.DELETE_ALL_CATEGORIES
                    }
                }
                cancelButton(backText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryListReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_LIST

        message {
            val lang = context.userInfo?.language
            val categories = context.categories
            val categoryType = context.categoryTypeInput
            val backText = localizationService.t("common.back", lang)

            text = if (categories.isEmpty()) {
                val emptyKey = listEmptyKey(categoryType)
                if (emptyKey != null) {
                    localizationService.t(emptyKey, lang)
                } else {
                    CATEGORY_LIST_TITLE_FALLBACK
                }
            } else {
                val titleKey = listTitleKey(categoryType)
                val title = if (titleKey != null) localizationService.t(titleKey, lang) else CATEGORY_LIST_TITLE_FALLBACK
                val subtitle = localizationService.t("category.list.subtitle", lang)
                "$title\n\n$subtitle"
            }

            keyboard {
                if (categories.isNotEmpty()) {
                    categories.forEach { category ->
                        buttonRow {
                            button {
                                val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                                text = "$icon ${category.name}"
                                type = MoneyManagerButtonType.CATEGORY_ITEM
                            }
                        }
                    }
                }
                backButton(backText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryActionsReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_ACTIONS

        message {
            val lang = context.userInfo?.language

            if (context.textInputResponse) {
                newPinnedMessage = true
                context.textInputResponse = false
            }

            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON
            val headerKey = actionsHeaderKey(category?.type)
            val header = if (headerKey != null) localizationService.t(headerKey, lang) else ""

            val confirmation = context.renameConfirmation?.let { "\n\n$it" } ?: ""
            context.renameConfirmation = null

            val chooseAction = localizationService.t("category.actions.choose_action", lang)
            val editNameText = localizationService.t("category.actions.button.edit_name", lang)
            val editIconText = localizationService.t("category.actions.button.edit_icon", lang)
            val deleteText = localizationService.t("category.actions.button.delete", lang)
            val backText = localizationService.t("common.back_to_list", lang)

            text = """
                |$header: $icon ${category?.name}
                |
                |$chooseAction$confirmation
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = editNameText
                        type = MoneyManagerButtonType.EDIT_CATEGORY
                    }
                }
                buttonRow {
                    button {
                        text = editIconText
                        type = MoneyManagerButtonType.EDIT_CATEGORY_ICON
                    }
                }
                buttonRow {
                    button {
                        text = deleteText
                        type = MoneyManagerButtonType.DELETE_CATEGORY_BUTTON
                    }
                }
                backButton(backText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryEditIconReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_EDIT_ICON

        message {
            val lang = context.userInfo?.language
            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON
            val cancelText = localizationService.t("common.cancel", lang)

            val title = localizationService.t("category.edit_icon.title", lang)
            val current = localizationService.t("category.edit_icon.current", lang, icon, category?.name ?: "")
            val prompt = localizationService.t("category.edit_icon.prompt", lang)
            val errorText = if (context.iconInputError) {
                localizationService.t("category.edit_icon.error", lang)
            } else ""
            context.iconInputError = false

            text = """
                |$title
                |
                |$current
                |
                |$prompt$errorText
            """.trimMargin()

            keyboard {
                cancelButton(cancelText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryEditNameReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_EDIT_NAME

        message {
            val lang = context.userInfo?.language
            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON
            val cancelText = localizationService.t("common.cancel", lang)

            val title = localizationService.t("category.edit_name.title", lang)
            val current = localizationService.t("category.edit_name.current", lang, icon, category?.name ?: "")
            val prompt = localizationService.t("category.edit_name.prompt", lang)

            text = """
                |$title
                |
                |$current
                |
                |$prompt
            """.trimMargin()

            keyboard {
                cancelButton(cancelText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryDeleteConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_DELETE_CONFIRM

        message {
            val lang = context.userInfo?.language
            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON
            val confirmText = localizationService.t("common.confirm.delete_yes", lang)
            val cancelText = localizationService.t("common.cancel", lang)

            text = localizationService.t("category.delete.text", lang, icon, category?.name ?: "")

            keyboard {
                confirmAndCancelButtons(confirmText = confirmText, cancelText = cancelText)
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryDeleteAllConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM

        message {
            val lang = context.userInfo?.language
            val categories = context.categories
            val categoryCount = categories.size
            val backText = localizationService.t("common.back", lang)

            text = if (categoryCount == 0) {
                localizationService.t("category.delete_all.empty", lang)
            } else {
                val expenseCount = categories.count { it.type == CategoryType.EXPENSE }
                val incomeCount = categories.count { it.type == CategoryType.INCOME }
                localizationService.t(
                    "category.delete_all.confirm.text", lang,
                    categoryCount, expenseCount, incomeCount
                )
            }

            keyboard {
                if (categoryCount > 0) {
                    val confirmText = localizationService.t("category.delete_all.button.confirm", lang)
                    val cancelText = localizationService.t("common.cancel", lang)
                    confirmAndCancelButtons(confirmText = confirmText, cancelText = cancelText)
                } else {
                    backButton(backText)
                }
            }
        }
    }
}

private fun createTitleKey(type: CategoryType?): String = when (type) {
    CategoryType.INCOME -> "category.create.title.income"
    else -> "category.create.title.expense"
}

private fun createSuccessKey(type: CategoryType?): String = when (type) {
    CategoryType.INCOME -> "category.create.result.success.income"
    else -> "category.create.result.success.expense"
}

private fun listTitleKey(type: CategoryType?): String? = when (type) {
    CategoryType.EXPENSE -> "category.list.title.expense"
    CategoryType.INCOME -> "category.list.title.income"
    else -> null
}

private fun listEmptyKey(type: CategoryType?): String? = when (type) {
    CategoryType.EXPENSE -> "category.list.empty.expense"
    CategoryType.INCOME -> "category.list.empty.income"
    else -> null
}

private fun actionsHeaderKey(type: CategoryType?): String? = when (type) {
    CategoryType.EXPENSE -> "category.actions.header.expense"
    CategoryType.INCOME -> "category.actions.header.income"
    else -> null
}

private fun quickTemplatesFor(type: CategoryType?): List<QuickCategoryTemplate> = when (type) {
    CategoryType.EXPENSE -> QuickTemplates.EXPENSE_CATEGORIES
    CategoryType.INCOME -> QuickTemplates.INCOME_CATEGORIES
    else -> emptyList()
}

private fun formatQuickButton(
    template: QuickCategoryTemplate,
    language: String?,
    localizationService: LocalizationService
): String {
    val name = localizationService.t(template.nameKey, language)
    return "${template.icon} $name"
}
