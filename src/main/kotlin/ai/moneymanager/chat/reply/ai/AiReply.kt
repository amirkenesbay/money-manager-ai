package ai.moneymanager.chat.reply.ai

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.chat.transition.ai.rankCategoriesByProposed
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.KeyboardBuilder
import kz.rmr.chatmachinist.api.reply.RepliesBuilder
import java.math.BigDecimal

private const val PICKER_TOP_N = 5
private const val BATCH_LINE_SEPARATOR = "\n"
private const val BATCH_SECTION_SEPARATOR = "\n\n"
private const val BATCH_TOTALS_SEPARATOR = " · "
private const val CONFIRM_YES_KEY = "ai.confirm.button.yes"
private const val HINTS_BUTTON_KEY = "ai.mode.button.hints"
private const val CONFIRM_SAVE_ALL_KEY = "ai.confirm.button.save_all"
private const val BATCH_TITLE_KEY = "ai.confirm.batch.title"
private const val BATCH_NOTES_KEY = "ai.confirm.batch.notes"
private const val BATCH_TOTALS_EXPENSE_KEY = "ai.batch.totals.expense"
private const val BATCH_TOTALS_INCOME_KEY = "ai.batch.totals.income"
private const val CREATE_NEW_CATEGORY_KEY = "ai.confirm.button.create_new_category"
private const val PICK_DIFFERENT_KEY = "ai.confirm.button.pick_different_category"
private const val USE_EXISTING_KEY = "ai.confirm.button.use_existing_category"
private const val PICKER_TITLE_KEY = "ai.picker.category.title"
private const val PICKER_SHOW_ALL_KEY = "ai.picker.category.show_all"
private const val PICKER_BACK_KEY = "ai.picker.category.back"

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiModeReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_MODE

        message {
            newMessage = false
            val lang = context.userInfo?.language

            text = localizationService.t("ai.mode.intro", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t(HINTS_BUTTON_KEY, lang)
                        type = MoneyManagerButtonType.WHAT_TO_ASK
                    }
                }
                backButton(text = localizationService.t("common.back_to_menu", lang))
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiConfirmReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_CONFIRM

        message {
            newMessage = true
            val ctx = context
            val lang = ctx.userInfo?.language
            val action = ctx.pendingAiAction

            val description = action
                ?.describe(localizationService, lang)
                ?: localizationService.t("ai.confirm.fallback", lang)

            text = localizationService.t("ai.confirm.title", lang, description)

            keyboard {
                val transactionAction = action as? AiPendingAction.TransactionAction
                if (transactionAction != null) {
                    renderTransactionConfirmButtons(transactionAction, lang, localizationService, ctx)
                } else {
                    buttonRow {
                        button {
                            text = localizationService.t(CONFIRM_YES_KEY, lang)
                            type = MoneyManagerButtonType.CONFIRM_AI_ACTION
                        }
                        button {
                            text = localizationService.t("common.cancel", lang)
                            type = MoneyManagerButtonType.CANCEL
                        }
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiConfirmBatchReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_CONFIRM_BATCH

        message {
            newMessage = true
            val ctx = context
            val lang = ctx.userInfo?.language

            val items = ctx.pendingAiActions
                .mapIndexed { index, action -> "${index + 1}. ${batchItemLine(action, localizationService, lang)}" }
                .joinToString(BATCH_LINE_SEPARATOR)
            val body = buildString {
                append(items)
                val totals = batchTotalsLine(ctx.pendingAiActions, localizationService, lang)
                if (totals.isNotEmpty()) append(BATCH_SECTION_SEPARATOR).append(totals)
                if (ctx.aiBatchNotes.isNotEmpty()) {
                    append(localizationService.t(BATCH_NOTES_KEY, lang, ctx.aiBatchNotes.joinToString(BATCH_LINE_SEPARATOR)))
                }
            }

            text = localizationService.t(BATCH_TITLE_KEY, lang, body)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t(CONFIRM_SAVE_ALL_KEY, lang)
                        type = MoneyManagerButtonType.CONFIRM_AI_ACTION
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.cancel", lang)
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiTransactionPickCategoryReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY

        message {
            newMessage = true
            val lang = context.userInfo?.language
            val candidates = transactionPickerCandidates(context)
            val proposedName = proposedCategoryName(context)
            val ranked = if (proposedName != null) {
                rankCategoriesByProposed(proposedName, candidates, PICKER_TOP_N)
            } else {
                candidates.take(PICKER_TOP_N)
            }

            text = localizationService.t(PICKER_TITLE_KEY, lang)

            keyboard {
                renderCategoryButtons(ranked)
                if (candidates.size > ranked.size) {
                    buttonRow {
                        button {
                            text = localizationService.t(PICKER_SHOW_ALL_KEY, lang)
                            type = MoneyManagerButtonType.AI_PICK_SHOW_ALL
                        }
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t(PICKER_BACK_KEY, lang)
                        type = MoneyManagerButtonType.AI_PICK_BACK_TO_CONFIRM
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiTransactionPickCategoryAllReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_TRANSACTION_PICK_CATEGORY_ALL

        message {
            newMessage = true
            val lang = context.userInfo?.language
            val candidates = transactionPickerCandidates(context)

            text = localizationService.t(PICKER_TITLE_KEY, lang)

            keyboard {
                renderCategoryButtons(candidates)
                buttonRow {
                    button {
                        text = localizationService.t(PICKER_BACK_KEY, lang)
                        type = MoneyManagerButtonType.AI_PICK_BACK_TO_CONFIRM
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.aiResultReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.AI_RESULT

        message {
            newMessage = false
            val lang = context.userInfo?.language

            text = context.aiResultMessage ?: localizationService.t("ai.result.empty", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("common.back_to_menu", lang)
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

private fun KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.renderTransactionConfirmButtons(
    action: AiPendingAction.TransactionAction,
    lang: String?,
    localizationService: LocalizationService,
    context: MoneyManagerContext
) {
    val isNewCategory = action is AiPendingAction.TransactionAction.AddWithNewCategory
    val candidatesOfType = countCandidatesOfType(context, action.type)
    val canOfferPicker = when (action) {
        is AiPendingAction.TransactionAction.Add -> candidatesOfType > 1
        is AiPendingAction.TransactionAction.AddWithNewCategory -> candidatesOfType > 0
    }

    val confirmKey = if (isNewCategory) CREATE_NEW_CATEGORY_KEY else CONFIRM_YES_KEY
    val pickerKey = if (isNewCategory) USE_EXISTING_KEY else PICK_DIFFERENT_KEY

    buttonRow {
        button {
            text = localizationService.t(confirmKey, lang)
            type = MoneyManagerButtonType.CONFIRM_AI_ACTION
        }
        if (canOfferPicker) {
            button {
                text = localizationService.t(pickerKey, lang)
                type = MoneyManagerButtonType.AI_PICK_DIFFERENT_CATEGORY
            }
        }
    }
    buttonRow {
        button {
            text = localizationService.t("common.cancel", lang)
            type = MoneyManagerButtonType.CANCEL
        }
    }
}

private fun KeyboardBuilder<MoneyManagerState, MoneyManagerContext>.renderCategoryButtons(
    categories: List<Category>
) {
    categories.forEach { category ->
        buttonRow {
            button {
                val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                text = "$icon ${category.name}"
                type = MoneyManagerButtonType.AI_PICK_CATEGORY_ITEM
            }
        }
    }
}

private fun transactionPickerCandidates(context: MoneyManagerContext): List<Category> {
    val action = context.pendingAiAction as? AiPendingAction.TransactionAction ?: return emptyList()
    return context.aiCategoriesCache.orEmpty().filter { it.type == action.type }
}

private fun proposedCategoryName(context: MoneyManagerContext): String? =
    (context.pendingAiAction as? AiPendingAction.TransactionAction)?.categoryDisplayName

private fun countCandidatesOfType(context: MoneyManagerContext, type: CategoryType): Int =
    context.aiCategoriesCache.orEmpty().count { it.type == type }

private fun batchItemLine(
    action: AiPendingAction,
    localizationService: LocalizationService,
    lang: String?
): String = when (action) {
    is AiPendingAction.TransactionAction -> action.describeBatchItem(localizationService, lang)
    else -> action.describe(localizationService, lang)
}

private fun batchTotalsLine(
    actions: List<AiPendingAction>,
    localizationService: LocalizationService,
    lang: String?
): String {
    val transactions = actions.filterIsInstance<AiPendingAction.TransactionAction>()
    val parts = listOfNotNull(
        batchTotalPart(transactions, CategoryType.EXPENSE, BATCH_TOTALS_EXPENSE_KEY, localizationService, lang),
        batchTotalPart(transactions, CategoryType.INCOME, BATCH_TOTALS_INCOME_KEY, localizationService, lang)
    )
    return parts.joinToString(BATCH_TOTALS_SEPARATOR)
}

private fun batchTotalPart(
    transactions: List<AiPendingAction.TransactionAction>,
    type: CategoryType,
    key: String,
    localizationService: LocalizationService,
    lang: String?
): String? = transactions
    .filter { it.type == type }
    .sumOf { it.amount }
    .takeIf { it > 0 }
    ?.let { localizationService.t(key, lang, formatAmount(BigDecimal.valueOf(it))) }
