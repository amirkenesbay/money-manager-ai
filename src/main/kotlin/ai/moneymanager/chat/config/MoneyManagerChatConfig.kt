package ai.moneymanager.chat.config

import ai.moneymanager.chat.dialog.moneyManagerDialog
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
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
    private val categoryService: CategoryService
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

            moneyManagerDialog(userInfoService, groupService, categoryService)
        }
}