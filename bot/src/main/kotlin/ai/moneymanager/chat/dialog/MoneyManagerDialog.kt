package ai.moneymanager.chat.dialog

import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.chat.reply.finance.FinanceHistoryFormatter
import ai.moneymanager.chat.telegram.toTelegramProfile
import ai.moneymanager.chat.transition.admin.adminDialogTransitions
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
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.chat.state.MoneyManagerButtonType
import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.state.PersistentAction
import ai.moneymanager.domain.model.StartParameters
import ai.moneymanager.chat.telegram.AdminService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.chat.reply.finance.FinanceReportFormatter
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.chat.telegram.TelegramFileService
import ai.moneymanager.chat.telegram.ADD_EXPENSE_BUTTON_TEXT
import ai.moneymanager.chat.telegram.ADD_INCOME_BUTTON_TEXT
import ai.moneymanager.chat.telegram.AI_BUTTON_TEXT
import ai.moneymanager.chat.telegram.MENU_BUTTON_TEXT
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.OnboardingService
import ai.moneymanager.chat.telegram.PRO_MODE_BUTTON_TEXT
import ai.moneymanager.chat.telegram.PersistentMenuKeyboardService
import ai.moneymanager.chat.telegram.MiniAppGateway
import ai.moneymanager.chat.transition.common.inviteTokenOf
import ai.moneymanager.chat.transition.gateway.miniAppGatewayDialog
import ai.moneymanager.chat.transition.gateway.miniAppHintTransitions
import ai.moneymanager.chat.transition.gateway.toGatewayOrMenu
import ai.moneymanager.chat.transition.gateway.toHome
import kz.rmr.chatmachinist.model.ActionContext
import ai.moneymanager.chat.telegram.REPORT_BUTTON_TEXT
import ai.moneymanager.chat.telegram.SETTINGS_BUTTON_TEXT
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.chat.telegram.DebtNotifier
import ai.moneymanager.chat.telegram.DigestNotifier
import ai.moneymanager.chat.telegram.RecurringNotifier
import ai.moneymanager.chat.transition.digest.startLinkDialog
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
    financeHistoryFormatter: FinanceHistoryFormatter,
    financeReportFormatter: FinanceReportFormatter,
    financeReportService: FinanceReportService,
    notificationService: NotificationService,
    localizationService: LocalizationService,
    aiActionExecutor: AiActionExecutor,
    aiRequestHandler: AiRequestHandler,
    persistentMenuKeyboardService: PersistentMenuKeyboardService,
    adminService: AdminService,
    subscriptionLimitsService: SubscriptionLimitsService,
    onboardingService: OnboardingService,
    miniAppGateway: MiniAppGateway,
    digestNotifier: DigestNotifier,
    recurringNotifier: RecurringNotifier,
    debtNotifier: DebtNotifier,
) {
    startLinkDialog(userInfoService, digestNotifier, recurringNotifier, debtNotifier, telegramFileService, miniAppGateway)
    miniAppGatewayDialog(miniAppGateway, userInfoService, adminService)

    dialog {
        name = "Money Manager Dialog"

        miniAppHintTransitions(miniAppGateway)
        startMoneyManagerDialogTransition(userInfoService, groupService, categoryService, financeOperationService, telegramFileService, persistentMenuKeyboardService, aiActionExecutor, miniAppGateway)
        joinGroupDialogTransitions(groupService, userInfoService, financeOperationService, localizationService, onboardingService, miniAppGateway)
        settingsDialogTransitions()
        languageDialogTransitions(onboardingService)
        currencyDialogTransitions(groupService, subscriptionLimitsService)
        balanceDialogTransitions(groupService, userInfoService, financeOperationService, miniAppGateway)
        groupDialogTransitions(groupService, categoryService, userInfoService, localizationService)
        categoryDialogTransitions(categoryService, groupService, localizationService, subscriptionLimitsService)
        financeDialogTransitions(categoryService, financeOperationService, financeHistoryService, financeHistoryFormatter, financeReportFormatter, financeReportService, userInfoService, groupService, subscriptionLimitsService, localizationService)
        notificationDialogTransitions(notificationService, userInfoService, subscriptionLimitsService)
        aiDialogTransitions(aiActionExecutor, aiRequestHandler, localizationService)
        adminDialogTransitions(adminService, userInfoService)
    }
}

private val PERSISTENT_BUTTON_ACTIONS: Map<String, PersistentAction> = mapOf(
    SETTINGS_BUTTON_TEXT to PersistentAction.OPEN_SETTINGS,
    AI_BUTTON_TEXT to PersistentAction.OPEN_AI,
    REPORT_BUTTON_TEXT to PersistentAction.OPEN_REPORT,
    ADD_INCOME_BUTTON_TEXT to PersistentAction.ADD_INCOME,
    ADD_EXPENSE_BUTTON_TEXT to PersistentAction.ADD_EXPENSE,
    PRO_MODE_BUTTON_TEXT to PersistentAction.OPEN_PRO_INFO,
)

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.startMoneyManagerDialogTransition(
    userInfoService: UserInfoService,
    groupService: GroupService,
    categoryService: CategoryService,
    financeOperationService: FinanceOperationService,
    telegramFileService: TelegramFileService,
    persistentMenuKeyboardService: PersistentMenuKeyboardService,
    aiActionExecutor: AiActionExecutor,
    miniAppGateway: MiniAppGateway
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
            context.userInfo = userInfoService.getOrCreate(user.toTelegramProfile())
            context.pendingPersistentAction = PERSISTENT_BUTTON_ACTIONS[update.message?.text]
            context.financeOperationType = null
            if (!miniAppGateway.enabled) {
                update.message?.chatId?.let { persistentMenuKeyboardService.attach(it) }
            }

            val messageText = update.message?.text
            val token = inviteTokenOf(messageText)
            if (token != null) {
                context.pendingInviteToken = token
                val group = groupService.getGroupByToken(token)
                context.pendingGroup = group

                if (group != null) {
                    context.pendingGroupOwnerInfo = userInfoService.getUserInfoByTelegramId(group.ownerId)
                }
            } else if (messageText?.split(" ")?.getOrNull(1) == StartParameters.OPEN_FINANCE) {
                context.pendingPersistentAction = PersistentAction.OPEN_FINANCE
                val chatId = update.message?.chatId
                val messageId = update.message?.messageId
                if (chatId != null && messageId != null) {
                    telegramFileService.deleteMessage(chatId, messageId)
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
        name = "Open PRO info from persistent button"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted == true &&
                    context.pendingPersistentAction == PersistentAction.OPEN_PRO_INFO
            }
        }

        action {
            context.pendingPersistentAction = null
        }

        then {
            to = MoneyManagerState.PRO_INFO
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
            toHome(miniAppGateway)
        }
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.joinGroupDialogTransitions(
    groupService: GroupService,
    userInfoService: UserInfoService,
    financeOperationService: FinanceOperationService,
    localizationService: LocalizationService,
    onboardingService: OnboardingService,
    miniAppGateway: MiniAppGateway
) {
    transition {
        name = "Confirm join group"

        condition {
            from = MoneyManagerState.GROUP_JOIN_CONFIRM
            button = MoneyManagerButtonType.CONFIRM_JOIN
        }

        action {
            val joinedGroup = context.pendingInviteToken?.let { token ->
                joinPendingGroup(token, groupService, userInfoService, onboardingService, localizationService)
            }
            loadCurrentBalance(groupService, financeOperationService)
            if (miniAppGateway.enabled) announceJoin(miniAppGateway, joinedGroup?.name)
        }

        then {
            toGatewayOrMenu(miniAppGateway)
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
            if (miniAppGateway.enabled) {
                to = MoneyManagerState.STARTED
                noReply = true
                trigger { sameDialog = true }
            } else {
                to = MoneyManagerState.MENU
            }
        }
    }
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.announceJoin(miniAppGateway: MiniAppGateway, groupName: String?) {
    if (groupName == null) {
        miniAppGateway.sendHint(chat.id, user.id)
        return
    }
    miniAppGateway.sendJoined(chat.id, user.id, groupName)
}

private fun ActionContext<MoneyManagerState, MoneyManagerContext>.joinPendingGroup(
    token: String,
    groupService: GroupService,
    userInfoService: UserInfoService,
    onboardingService: OnboardingService,
    localizationService: LocalizationService
): MoneyGroup? {
    val userId = user.id
    ensurePersonalGroupOnJoin(userId, userInfoService, onboardingService, localizationService)

    val joinedGroup = groupService.joinGroup(userId, token)
    context.currentGroup = joinedGroup

    userInfoService.markOnboardingCompleted(userId)
    val updatedUserInfo = userInfoService.getOrCreate(user.toTelegramProfile())
    context.userInfo = updatedUserInfo

    log.info("User joined group: groupId=${joinedGroup?.id}, activeGroupId=${updatedUserInfo.activeGroupId}")
    return joinedGroup
}

private fun ensurePersonalGroupOnJoin(
    userId: Long,
    userInfoService: UserInfoService,
    onboardingService: OnboardingService,
    localizationService: LocalizationService
) {
    val telegramLanguageCode = userInfoService.getUserInfoByTelegramId(userId)?.languageCode
    val language = localizationService.resolveLanguage(selected = null, telegramLanguageCode = telegramLanguageCode)
    onboardingService.ensurePersonalGroup(userId, language)
}
