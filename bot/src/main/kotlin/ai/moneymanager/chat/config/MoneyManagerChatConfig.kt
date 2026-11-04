package ai.moneymanager.chat.config

import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.chat.reply.finance.FinanceHistoryFormatter
import ai.moneymanager.chat.dialog.moneyManagerDialog
import ai.moneymanager.chat.transition.ai.AiActionExecutor
import ai.moneymanager.chat.transition.ai.AiRequestHandler
import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.AdminService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.chat.reply.finance.FinanceReportFormatter
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.OnboardingService
import ai.moneymanager.chat.telegram.DigestNotifier
import ai.moneymanager.chat.telegram.RecurringNotifier
import ai.moneymanager.chat.telegram.MiniAppGateway
import ai.moneymanager.chat.telegram.PersistentMenuKeyboardService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.chat.telegram.TelegramFileService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.api.transition.chat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class MoneyManagerChatConfig(
    private val userInfoService: UserInfoService,
    private val groupService: GroupService,
    private val categoryService: CategoryService,
    private val telegramFileService: TelegramFileService,
    private val financeOperationService: FinanceOperationService,
    private val financeHistoryService: FinanceHistoryService,
    private val financeHistoryFormatter: FinanceHistoryFormatter,
    private val financeReportFormatter: FinanceReportFormatter,
    private val financeReportService: FinanceReportService,
    private val notificationService: NotificationService,
    private val localizationService: LocalizationService,
    private val aiActionExecutor: AiActionExecutor,
    private val aiRequestHandler: AiRequestHandler,
    private val persistentMenuKeyboardService: PersistentMenuKeyboardService,
    private val adminService: AdminService,
    private val subscriptionLimitsService: SubscriptionLimitsService,
    private val onboardingService: OnboardingService,
    private val miniAppGateway: MiniAppGateway,
    private val digestNotifier: DigestNotifier,
    private val recurringNotifier: RecurringNotifier,
) {
    @Bean
    fun moneyManagerChat(): ChatBuilder<MoneyManagerState, MoneyManagerContext> =
        chat {
            name = "Money Manager Chat"

            initialContext {
                MoneyManagerContext()
            }

            commands {
                command {
                    text = "/start"
                    description = "Открыть Меню"
                }
            }

            moneyManagerDialog(
                userInfoService, groupService, categoryService, telegramFileService,
                financeOperationService, financeHistoryService, financeHistoryFormatter, financeReportFormatter, financeReportService,
                notificationService, localizationService, aiActionExecutor, aiRequestHandler,
                persistentMenuKeyboardService, adminService, subscriptionLimitsService, onboardingService, miniAppGateway, digestNotifier, recurringNotifier
            )
        }
}
