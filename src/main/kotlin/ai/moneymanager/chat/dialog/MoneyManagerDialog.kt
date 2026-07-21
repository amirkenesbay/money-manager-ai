package ai.moneymanager.chat.dialog

import ai.moneymanager.chat.transition.ai.AiActionExecutor
import ai.moneymanager.chat.transition.ai.AiRequestHandler
import ai.moneymanager.chat.transition.ai.aiDialogTransitions
import ai.moneymanager.chat.transition.balance.balanceDialogTransitions
import ai.moneymanager.chat.transition.balance.loadCurrentBalance
import ai.moneymanager.chat.transition.category.categoryDialogTransitions
import ai.moneymanager.chat.transition.finance.financeDialogTransitions
import ai.moneymanager.chat.transition.group.groupDialogTransitions
import ai.moneymanager.chat.transition.nlp.nlpDialogTransitions
import ai.moneymanager.chat.transition.notification.notificationDialogTransitions
import ai.moneymanager.chat.transition.finance.loadFinanceCategories
import ai.moneymanager.chat.transition.settings.currencyDialogTransitions
import ai.moneymanager.chat.transition.settings.languageDialogTransitions
import ai.moneymanager.chat.transition.settings.settingsDialogTransitions
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.PersistentAction
import ai.moneymanager.domain.model.StartParameters
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.ADD_EXPENSE_BUTTON_TEXT
import ai.moneymanager.service.ADD_INCOME_BUTTON_TEXT
import ai.moneymanager.service.AI_BUTTON_TEXT
import ai.moneymanager.service.MENU_BUTTON_TEXT
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.PersistentMenuKeyboardService
import ai.moneymanager.service.REPORT_BUTTON_TEXT
import ai.moneymanager.service.SETTINGS_BUTTON_TEXT
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MoneyManagerDialog")

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.moneyManagerDialog(
    userInfoService: UserInfoService,
    groupService: GroupService,
    categoryService: CategoryService,
    telegramFileService: TelegramFileService,
    financeOperationService: FinanceOperationService,
    financeHistoryService: FinanceHistoryService,
    financeReportService: FinanceReportService,
    notificationService: NotificationService,
    localizationService: LocalizationService,
    aiActionExecutor: AiActionExecutor,
    aiRequestHandler: AiRequestHandler,
    persistentMenuKeyboardService: PersistentMenuKeyboardService
) {
    dialog {
        name = "Money Manager Dialog"

        startMoneyManagerDialogTransition(userInfoService, groupService, categoryService, financeOperationService, telegramFileService, persistentMenuKeyboardService, aiActionExecutor)
        joinGroupDialogTransitions(groupService, userInfoService, financeOperationService, localizationService)
        settingsDialogTransitions()
        languageDialogTransitions(userInfoService, groupService, localizationService)
        currencyDialogTransitions(groupService)
        balanceDialogTransitions(groupService, userInfoService, financeOperationService)
        groupDialogTransitions(groupService, categoryService, userInfoService, localizationService)
        categoryDialogTransitions(categoryService, groupService, localizationService)
        financeDialogTransitions(categoryService, financeOperationService, financeHistoryService, financeReportService, userInfoService, groupService)
        notificationDialogTransitions(notificationService, userInfoService)
        aiDialogTransitions(aiActionExecutor, aiRequestHandler, localizationService)
        // Legacy NLP disabled for now
        // nlpDialogTransitions(commandParserService, groupService, userInfoService, telegramFileService, geminiService)
    }
}

/** Reply-кнопка → действие, которое нужно выполнить сразу после /start. */
private val PERSISTENT_BUTTON_ACTIONS: Map<String, PersistentAction> = mapOf(
    SETTINGS_BUTTON_TEXT to PersistentAction.OPEN_SETTINGS,
    AI_BUTTON_TEXT to PersistentAction.OPEN_AI,
    REPORT_BUTTON_TEXT to PersistentAction.OPEN_REPORT,
    ADD_INCOME_BUTTON_TEXT to PersistentAction.ADD_INCOME,
    ADD_EXPENSE_BUTTON_TEXT to PersistentAction.ADD_EXPENSE,
)

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.startMoneyManagerDialogTransition(
    userInfoService: UserInfoService,
    groupService: GroupService,
    categoryService: CategoryService,
    financeOperationService: FinanceOperationService,
    telegramFileService: TelegramFileService,
    persistentMenuKeyboardService: PersistentMenuKeyboardService,
    aiActionExecutor: AiActionExecutor
) {
    transition {
        name = "Start Money Manager Dialog"
        startDialog = true

        condition {
            eventType = EventType.COMMAND

            guard {
                update.message?.text?.startsWith("/start") == true
            }
        }

        condition {
            eventType = EventType.TEXT

            guard {
                update.message?.text == MENU_BUTTON_TEXT
            }
        }

        condition {
            eventType = EventType.TEXT

            guard {
                update.message?.text in PERSISTENT_BUTTON_ACTIONS
            }
        }

        action {
            context.isActive = true
            context.userInfo = userInfoService.getUserInfo(user)
            context.pendingPersistentAction = PERSISTENT_BUTTON_ACTIONS[update.message?.text]
            context.financeOperationType = null
            update.message?.chatId?.let { persistentMenuKeyboardService.attach(it) }

            val messageText = update.message?.text
            if (messageText != null) {
                val parts = messageText.split(" ")
                if (parts.size == 2 && parts[1].startsWith("join_")) {
                    val token = parts[1].removePrefix("join_")
                    context.pendingInviteToken = token
                    val group = groupService.getGroupByToken(token)
                    context.pendingGroup = group

                    if (group != null) {
                        context.pendingGroupOwnerInfo = userInfoService.getUserInfoByTelegramId(group.ownerId)
                    }
                } else if (parts.size == 2 && parts[1] == StartParameters.OPEN_FINANCE) {
                    context.pendingPersistentAction = PersistentAction.OPEN_FINANCE
                    val chatId = update.message?.chatId
                    val messageId = update.message?.messageId
                    if (chatId != null && messageId != null) {
                        telegramFileService.deleteMessage(chatId, messageId)
                    }
                }
            }
        }

        then {
            to = MoneyManagerState.STARTED
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Show group join confirmation"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup != null
            }
        }

        then {
            to = MoneyManagerState.GROUP_JOIN_CONFIRM
        }
    }

    transition {
        name = "Open settings from persistent button"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null && context.pendingPersistentAction == PersistentAction.OPEN_SETTINGS
            }
        }

        action {
            context.pendingPersistentAction = null
        }

        then {
            to = MoneyManagerState.SETTINGS
        }
    }

    transition {
        name = "Show language picker on first start"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.pendingPersistentAction != PersistentAction.OPEN_SETTINGS &&
                    context.userInfo?.language == null
            }
        }

        then {
            to = MoneyManagerState.LANGUAGE_SELECT
        }
    }

    transition {
        name = "Show currency picker on first start"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.pendingPersistentAction != PersistentAction.OPEN_SETTINGS &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted != true
            }
        }

        then {
            to = MoneyManagerState.CURRENCY_SELECT
        }
    }

    transition {
        name = "Open AI mode from persistent button"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted == true &&
                    context.pendingPersistentAction == PersistentAction.OPEN_AI
            }
        }

        action {
            context.pendingPersistentAction = null
            aiActionExecutor.clear(context)
        }

        then {
            to = MoneyManagerState.AI_MODE
        }
    }

    transition {
        name = "Open report menu from persistent button"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted == true &&
                    context.pendingPersistentAction == PersistentAction.OPEN_REPORT
            }
        }

        action {
            context.pendingPersistentAction = null
        }

        then {
            to = MoneyManagerState.FINANCE_REPORT_MENU
        }
    }

    transition {
        name = "Prepare add income/expense from persistent button"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted == true &&
                    (context.pendingPersistentAction == PersistentAction.ADD_INCOME || context.pendingPersistentAction == PersistentAction.ADD_EXPENSE)
            }
        }

        action {
            context.financeOperationType = if (context.pendingPersistentAction == PersistentAction.ADD_INCOME) {
                CategoryType.INCOME
            } else {
                CategoryType.EXPENSE
            }
            context.pendingPersistentAction = null
            context.selectedCategory = null
            loadFinanceCategories(categoryService)
        }

        then {
            to = MoneyManagerState.STARTED
            noReply = true
            trigger { sameDialog = true }
        }
    }

    transition {
        name = "Route to category list from persistent button"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard { context.financeOperationType != null && context.categories.isNotEmpty() }
        }

        then {
            to = MoneyManagerState.FINANCE_SELECT_CATEGORY
        }
    }

    transition {
        name = "Route to no categories warning from persistent button"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard { context.financeOperationType != null && context.categories.isEmpty() }
        }

        then {
            to = MoneyManagerState.FINANCE_NO_CATEGORIES_WARNING
        }
    }

    transition {
        name = "Open finance from deep link"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted == true &&
                    context.pendingPersistentAction == PersistentAction.OPEN_FINANCE
            }
        }

        action {
            context.pendingPersistentAction = null
            loadCurrentBalance(groupService, financeOperationService)
        }

        then {
            to = MoneyManagerState.FINANCE_MANAGEMENT
        }
    }

    transition {
        name = "Open main menu"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted == true &&
                    context.pendingPersistentAction == null
            }
        }

        action {
            loadCurrentBalance(groupService, financeOperationService)
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.joinGroupDialogTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService,
    financeOperationService: FinanceOperationService,
    localizationService: LocalizationService
) {
    transition {
        name = "Confirm join group"

        condition {
            from = MoneyManagerState.GROUP_JOIN_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_JOIN
        }

        action {
            val userId = user.id
            val token = context.pendingInviteToken
            if (token != null) {
                ensurePersonalGroupOnJoin(userId, userInfoService, groupService, localizationService)

                val joinedGroup = groupService.joinGroup(userId, token)
                context.currentGroup = joinedGroup

                userInfoService.markOnboardingCompleted(userId)
                val updatedUserInfo = userInfoService.getUserInfo(user)
                context.userInfo = updatedUserInfo

                log.info("User joined group: groupId=${joinedGroup?.id}, activeGroupId=${updatedUserInfo.activeGroupId}")
            }
            loadCurrentBalance(groupService, financeOperationService)
        }

        then {
            to = MoneyManagerState.MENU
        }
    }

    transition {
        name = "Cancel join group"

        condition {
            from = MoneyManagerState.GROUP_JOIN_CONFIRM
            button = MoneyManagerButtonType.CANCEL
        }

        action {
            context.pendingInviteToken = null
            context.pendingGroup = null
            context.pendingGroupOwnerInfo = null
            loadCurrentBalance(groupService, financeOperationService)
        }

        then {
            to = MoneyManagerState.MENU
        }
    }
}

private const val PERSONAL_GROUP_NAME_KEY = "group.default.personal_name"

private fun ensurePersonalGroupOnJoin(
    userId: Long,
    userInfoService: UserInfoService,
    groupService: GroupService,
    localizationService: LocalizationService
) {
    if (groupService.getUserGroups(userId).isNotEmpty()) return
    val telegramLanguageCode = userInfoService.getUserInfoByTelegramId(userId)?.languageCode
    val resolvedLanguage = localizationService.resolveLanguage(selected = null, telegramLanguageCode = telegramLanguageCode)
    val personalName = localizationService.t(PERSONAL_GROUP_NAME_KEY, resolvedLanguage)
    groupService.createPersonalGroup(userId, personalName, resolvedLanguage)
}