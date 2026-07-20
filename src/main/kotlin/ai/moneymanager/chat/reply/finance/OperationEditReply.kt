package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.bold
import ai.moneymanager.chat.reply.common.cancelButton
import ai.moneymanager.chat.reply.common.categoryButtonText
import ai.moneymanager.chat.reply.common.code
import ai.moneymanager.chat.reply.common.confirmAndCancelButtons
import ai.moneymanager.chat.reply.common.dateFormatter
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.formatDescriptionSuffix
import ai.moneymanager.chat.reply.common.formatSignedAmount
import ai.moneymanager.chat.reply.common.operationListButtonText
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.ParseMode
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

private const val OPERATION_LIST_LIMIT = 10

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationListReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_LIST

        message {
            parseMode = ParseMode.HTML
            val lang = context.userInfo?.language
            val operations = context.operationsList.take(OPERATION_LIST_LIMIT)

            text = if (operations.isEmpty()) {
                localizationService.t("operation.list.empty", lang)
            } else {
                bold(localizationService.t("operation.list.title", lang))
            }

            keyboard {
                operations.forEach { operation ->
                    buttonRow {
                        button {
                            text = operationListItemText(operation)
                            type = MoneyManagerButtonType.OPERATION_LIST_ITEM
                        }
                    }
                }
                backButton(
                    text = localizationService.t("common.back_to_finance", lang),
                    type = MoneyManagerButtonType.BACK_TO_FINANCE
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationActionsReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_ACTIONS

        message {
            parseMode = ParseMode.HTML
            val lang = context.userInfo?.language
            val operation = context.selectedOperation

            text = if (operation == null) {
                localizationService.t("operation.not_found", lang)
            } else {
                buildOperationDetailsText(operation, localizationService, lang)
            }

            keyboard {
                if (operation != null) {
                    buttonRow {
                        button {
                            text = localizationService.t("operation.actions.button.edit_amount", lang)
                            type = MoneyManagerButtonType.OPERATION_EDIT_AMOUNT_BTN
                        }
                        button {
                            text = localizationService.t("operation.actions.button.edit_type", lang)
                            type = MoneyManagerButtonType.OPERATION_EDIT_TYPE_BTN
                        }
                    }
                    buttonRow {
                        button {
                            text = localizationService.t("operation.actions.button.edit_category", lang)
                            type = MoneyManagerButtonType.OPERATION_EDIT_CATEGORY_BTN
                        }
                        button {
                            text = localizationService.t("operation.actions.button.edit_date", lang)
                            type = MoneyManagerButtonType.OPERATION_EDIT_DATE_BTN
                        }
                    }
                    buttonRow {
                        button {
                            text = localizationService.t("operation.actions.button.edit_comment", lang)
                            type = MoneyManagerButtonType.OPERATION_EDIT_COMMENT_BTN
                        }
                        button {
                            text = localizationService.t("operation.actions.button.delete", lang)
                            type = MoneyManagerButtonType.OPERATION_DELETE_BTN
                        }
                    }
                }
                backButton(
                    text = localizationService.t("common.back_to_list", lang),
                    type = MoneyManagerButtonType.BACK_TO_OPERATION_LIST
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationEditAmountReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_EDIT_AMOUNT

        message {
            val lang = context.userInfo?.language
            val errorPrefix = if (context.operationEditError) {
                localizationService.t("operation.edit.amount.error", lang)
            } else ""
            text = errorPrefix + localizationService.t("operation.edit.amount.prompt", lang)

            keyboard {
                backButton(
                    text = localizationService.t("common.cancel", lang),
                    type = MoneyManagerButtonType.BACK_TO_OPERATION_ACTIONS
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationEditSelectTypeReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_EDIT_SELECT_TYPE

        message {
            val lang = context.userInfo?.language
            text = localizationService.t("operation.edit.type.prompt", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("category.button.type.expense_singular", lang)
                        type = MoneyManagerButtonType.OPERATION_TYPE_EXPENSE
                    }
                    button {
                        text = localizationService.t("category.button.type.income_singular", lang)
                        type = MoneyManagerButtonType.OPERATION_TYPE_INCOME
                    }
                }
                backButton(
                    text = localizationService.t("common.cancel", lang),
                    type = MoneyManagerButtonType.BACK_TO_OPERATION_ACTIONS
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationEditSelectCategoryReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_EDIT_SELECT_CATEGORY

        message {
            val lang = context.userInfo?.language
            val operation = context.selectedOperation
            val operationType = operation?.type
            val matching = context.categories.filter { it.type == operationType }

            text = localizationService.t("operation.edit.category.prompt", lang)

            keyboard {
                matching.forEach { category ->
                    buttonRow {
                        button {
                            text = categoryButtonText(category)
                            type = MoneyManagerButtonType.OPERATION_CATEGORY_ITEM
                        }
                    }
                }
                backButton(
                    text = localizationService.t("common.cancel", lang),
                    type = MoneyManagerButtonType.BACK_TO_OPERATION_ACTIONS
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationEditDateReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_EDIT_DATE

        message {
            val lang = context.userInfo?.language
            val errorPrefix = if (context.operationEditError) {
                localizationService.t("operation.edit.date.error", lang)
            } else ""
            text = errorPrefix + localizationService.t("operation.edit.date.prompt", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.date.button.today", lang)
                        type = MoneyManagerButtonType.QUICK_DATE_TODAY
                    }
                    button {
                        text = localizationService.t("finance.date.button.yesterday", lang)
                        type = MoneyManagerButtonType.QUICK_DATE_YESTERDAY
                    }
                }
                backButton(
                    text = localizationService.t("common.cancel", lang),
                    type = MoneyManagerButtonType.BACK_TO_OPERATION_ACTIONS
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationEditCommentReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_EDIT_COMMENT

        message {
            val lang = context.userInfo?.language
            text = localizationService.t("operation.edit.comment.prompt", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.comment.button.skip", lang)
                        type = MoneyManagerButtonType.SKIP_COMMENT
                    }
                }
                backButton(
                    text = localizationService.t("common.cancel", lang),
                    type = MoneyManagerButtonType.BACK_TO_OPERATION_ACTIONS
                )
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.operationDeleteConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.OPERATION_DELETE_CONFIRM

        message {
            parseMode = ParseMode.HTML
            val lang = context.userInfo?.language
            val operation = context.selectedOperation

            text = if (operation == null) {
                localizationService.t("operation.not_found", lang)
            } else {
                localizationService.t(
                    "operation.delete.confirm",
                    lang,
                    operation.categoryIcon ?: DEFAULT_CATEGORY_ICON,
                    escapeHtml(operation.categoryName),
                    formatSignedAmount(operation.type, operation.amount)
                )
            }

            keyboard {
                confirmAndCancelButtons(
                    confirmText = localizationService.t("common.confirm.delete_yes", lang),
                    confirmType = MoneyManagerButtonType.CONFIRM_DELETE,
                    cancelText = localizationService.t("common.cancel", lang)
                )
            }
        }
    }
}

private fun operationListItemText(operation: FinanceOperationEntity): String = operationListButtonText(
    date = operation.operationDate.format(dateFormatter),
    icon = operation.categoryIcon,
    categoryName = operation.categoryName,
    signedAmount = formatSignedAmount(operation.type, operation.amount)
)

private fun buildOperationDetailsText(
    operation: FinanceOperationEntity,
    localizationService: LocalizationService,
    lang: String?
): String {
    val icon = operation.categoryIcon ?: DEFAULT_CATEGORY_ICON
    val amount = code(formatSignedAmount(operation.type, operation.amount))
    val date = operation.operationDate.format(dateFormatter)
    val description = formatDescriptionSuffix(operation.description).let { if (it.isNotEmpty()) escapeHtml(it) else it }
    return bold(
        localizationService.t(
            "operation.details.title",
            lang,
            icon,
            escapeHtml(operation.categoryName),
            amount,
            date
        )
    ) + description
}
