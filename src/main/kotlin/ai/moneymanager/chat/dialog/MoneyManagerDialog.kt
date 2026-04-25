package ai.moneymanager.chat.dialog

import ai.moneymanager.chat.transition.ai.aiDialogTransitions
import ai.moneymanager.chat.transition.ai.handler.AiDomainHandler
import ai.moneymanager.chat.transition.balance.balanceDialogTransitions
import ai.moneymanager.chat.transition.balance.loadCurrentBalance
import ai.moneymanager.chat.transition.category.categoryDialogTransitions
import ai.moneymanager.chat.transition.finance.financeDialogTransitions
import ai.moneymanager.chat.transition.group.groupDialogTransitions
import ai.moneymanager.chat.transition.nlp.nlpDialogTransitions
import ai.moneymanager.chat.transition.notification.notificationDialogTransitions
import ai.moneymanager.chat.transition.settings.languageDialogTransitions
import ai.moneymanager.chat.transition.settings.settingsDialogTransitions
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.domain.model.StartParameters
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.GeminiService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.nlp.CommandParserService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MoneyManagerDialog")

fun ChatBuilder<MoneyManagerState, MoneyManagerContext>.moneyManagerDialog(
    userInfoService: UserInfoService,
    groupService: GroupService,
    categoryService: CategoryService,
    commandParserService: CommandParserService,
    telegramFileService: TelegramFileService,
    geminiService: GeminiService,
    financeOperationService: FinanceOperationService,
    financeHistoryService: FinanceHistoryService,
    financeReportService: FinanceReportService,
    notificationService: NotificationService,
    localizationService: LocalizationService,
    aiPromptService: AiPromptService,
    aiDomainHandlers: List<AiDomainHandler>
) {
    dialog {
        name = "Money Manager Dialog"

        startMoneyManagerDialogTransition(userInfoService, groupService, financeOperationService, telegramFileService)
        joinGroupDialogTransitions(groupService, userInfoService, financeOperationService, localizationService)
        settingsDialogTransitions()
        languageDialogTransitions(userInfoService, groupService, localizationService)
        balanceDialogTransitions(groupService, userInfoService, financeOperationService)
        groupDialogTransitions(groupService, categoryService, userInfoService, localizationService)
        categoryDialogTransitions(categoryService, groupService, localizationService)
        financeDialogTransitions(categoryService, financeOperationService, financeHistoryService, financeReportService, userInfoService, groupService)
        notificationDialogTransitions(notificationService, userInfoService)
        aiDialogTransitions(commandParserService, telegramFileService, geminiService, localizationService, aiPromptService, aiDomainHandlers)
        // Legacy NLP disabled for now
        // nlpDialogTransitions(commandParserService, groupService, userInfoService, telegramFileService, geminiService)
    }
}

private fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.startMoneyManagerDialogTransition(
    userInfoService: UserInfoService,
    groupService: GroupService,
    financeOperationService: FinanceOperationService,
    telegramFileService: TelegramFileService
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

        action {
            context.isActive = true
            context.userInfo = userInfoService.getUserInfo(user)
            context.pendingOpenFinance = false

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
                    context.pendingOpenFinance = true
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
        name = "Show language picker on first start"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null && context.userInfo?.language == null
            }
        }

        then {
            to = MoneyManagerState.LANGUAGE_SELECT
        }
    }

    transition {
        name = "Show balance onboarding"

        condition {
            from = MoneyManagerState.STARTED
            eventType = EventType.TRIGGERED

            guard {
                context.pendingGroup == null &&
                    context.userInfo?.language != null &&
                    context.userInfo?.onboardingCompleted != true
            }
        }

        then {
            to = MoneyManagerState.BALANCE_ONBOARDING_PROMPT
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
                    context.pendingOpenFinance
            }
        }

        action {
            context.pendingOpenFinance = false
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
                    !context.pendingOpenFinance
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