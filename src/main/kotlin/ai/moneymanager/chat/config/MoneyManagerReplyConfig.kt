package ai.moneymanager.chat.config

import ai.moneymanager.chat.reply.ai.aiConfirmReply
import ai.moneymanager.chat.reply.ai.aiModeReply
import ai.moneymanager.chat.reply.ai.aiResultReply
import ai.moneymanager.chat.reply.category.*
import ai.moneymanager.chat.reply.finance.financeManagementReply
import ai.moneymanager.chat.reply.finance.financeOperationSavedReply
import ai.moneymanager.chat.reply.finance.financeHistoryReply
import ai.moneymanager.chat.reply.finance.financeReportMenuReply
import ai.moneymanager.chat.reply.finance.financeReportComparisonReply
import ai.moneymanager.chat.reply.finance.financeReportAnalyticsReply
import ai.moneymanager.chat.reply.finance.financeReportMembersReply
import ai.moneymanager.chat.reply.finance.financeReportCategorySelectReply
import ai.moneymanager.chat.reply.finance.financeReportCategoryViewReply
import ai.moneymanager.chat.reply.finance.financeHistorySelectStartDateReply
import ai.moneymanager.chat.reply.finance.financeHistorySelectEndDateReply
import ai.moneymanager.chat.reply.finance.financeHistoryCalendarReply
import ai.moneymanager.chat.reply.finance.financeHistorySelectYearReply
import ai.moneymanager.chat.reply.finance.financeHistorySelectMonthReply
import ai.moneymanager.chat.reply.finance.financeCalendarReply
import ai.moneymanager.chat.reply.finance.financeEnterAmountReply
import ai.moneymanager.chat.reply.finance.financeEnterCommentReply
import ai.moneymanager.chat.reply.finance.financeNoCategoriesWarningReply
import ai.moneymanager.chat.reply.finance.financeSelectCategoryReply
import ai.moneymanager.chat.reply.finance.financeSelectDateReply
import ai.moneymanager.chat.reply.finance.financeSelectMonthReply
import ai.moneymanager.chat.reply.finance.financeSelectYearReply
import ai.moneymanager.chat.reply.group.*
import ai.moneymanager.chat.reply.money.balanceOnboardingEnterAmountReply
import ai.moneymanager.chat.reply.money.balanceOnboardingPromptReply
import ai.moneymanager.chat.reply.money.balanceViewReply
import ai.moneymanager.chat.reply.money.menuReply
import ai.moneymanager.chat.reply.notification.*
import ai.moneymanager.chat.reply.nlp.nlpConfirmCreateGroupReply
import ai.moneymanager.chat.reply.nlp.nlpConfirmDeleteGroupReply
import ai.moneymanager.chat.reply.nlp.nlpResponseReply
import ai.moneymanager.chat.reply.settings.languageSelectReply
import ai.moneymanager.chat.reply.settings.settingsReply
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder
import kz.rmr.chatmachinist.api.reply.replies
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MoneyManagerReplyConfig {
    @Bean
    fun replyBuilder(
        financeHistoryService: FinanceHistoryService,
        localizationService: LocalizationService
    ): RepliesBuilder<MoneyManagerState, MoneyManagerContext> {
        return replies {
            chatName = "Money Manager Chat"

            // Money replies
            menuReply(localizationService)

            // Settings replies
            settingsReply(localizationService)
            languageSelectReply(localizationService)

            // Balance replies
            balanceOnboardingPromptReply()
            balanceOnboardingEnterAmountReply()
            balanceViewReply()

            // Finance replies
            financeManagementReply(financeHistoryService)
            financeSelectCategoryReply()
            financeNoCategoriesWarningReply()
            financeEnterAmountReply()
            financeEnterCommentReply()
            financeSelectDateReply()
            financeCalendarReply()
            financeSelectYearReply()
            financeSelectMonthReply()
            financeOperationSavedReply()

            // Finance report replies
            financeReportMenuReply()
            financeReportComparisonReply()
            financeReportAnalyticsReply()
            financeReportMembersReply()
            financeReportCategorySelectReply()
            financeReportCategoryViewReply()

            // Finance history replies
            financeHistoryReply()
            financeHistorySelectStartDateReply()
            financeHistorySelectEndDateReply()
            financeHistoryCalendarReply()
            financeHistorySelectYearReply()
            financeHistorySelectMonthReply()

            // Group replies
            groupManagementReply(localizationService)
            groupCreateEnterNameReply(localizationService)
            groupInviteShowReply(localizationService)
            groupJoinConfirmReply(localizationService)
            groupListReply(localizationService)
            groupActionsReply(localizationService)
            groupMembersReply(localizationService)
            groupEditEnterNameReply(localizationService)
            groupDeleteConfirmReply(localizationService)

            // Category replies
            categoryManagementReply()
            categoryNoGroupWarningReply()
            categoryCreateSelectTypeReply()
            categoryCreateEnterNameReply()
            categoryCreateResultReply()
            categoryListSelectTypeReply()
            categoryListReply()
            categoryActionsReply()
            categoryEditIconReply()
            categoryEditNameReply()
            categoryDeleteConfirmReply()
            categoryDeleteAllConfirmReply()

            // Notification replies
            notificationTimezoneSelectReply()
            notificationTimezoneExtendedReply()
            notificationCreateIconReply()
            notificationListReply()
            notificationActionsReply()
            notificationCreateNameReply()
            notificationCreateFrequencyReply()
            notificationCreateCustomUnitReply()
            notificationCreateCustomNReply()
            notificationCreateDayOfWeekReply()
            notificationCreateDayOfMonthReply()
            notificationCreateMonthReply()
            notificationCreateSelectHourReply()
            notificationCreateSelectMinuteReply()
            notificationCreateConfirmReply()
            notificationEditMenuReply()
            notificationEditNameReply()
            notificationDeleteConfirmReply()
            notificationDeleteAllConfirmReply()

            // AI replies
            aiModeReply()
            aiConfirmReply()
            aiResultReply()

            // Legacy NLP replies (disabled for now)
            // nlpConfirmCreateGroupReply()
            // nlpConfirmDeleteGroupReply()
            // nlpResponseReply()
        }
    }
}