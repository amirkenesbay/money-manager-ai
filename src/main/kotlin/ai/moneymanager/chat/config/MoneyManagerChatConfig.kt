package ai.moneymanager.chat.config

import ai.moneymanager.chat.dialog.moneyManagerDialog
import ai.moneymanager.chat.transition.ai.AiActionExecutor
import ai.moneymanager.chat.transition.ai.AiRequestHandler
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.TelegramFileService
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.ChatBuilder
import kz.rmr.chatmachinist.api.transition.chat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableMongoRepositories(basePackages = ["ai.moneymanager.repository"])
@EnableScheduling
class MoneyManagerChatConfig(
    private val userInfoService: UserInfoService,
    private val groupService: GroupService,
    private val categoryService: CategoryService,
    private val telegramFileService: TelegramFileService,
    private val financeOperationService: FinanceOperationService,
    private val financeHistoryService: FinanceHistoryService,
    private val financeReportService: FinanceReportService,
    private val notificationService: NotificationService,
    private val localizationService: LocalizationService,
    private val aiActionExecutor: AiActionExecutor,
    private val aiRequestHandler: AiRequestHandler
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
                financeOperationService, financeHistoryService, financeReportService,
                notificationService, localizationService, aiActionExecutor, aiRequestHandler
            )
        }
}
