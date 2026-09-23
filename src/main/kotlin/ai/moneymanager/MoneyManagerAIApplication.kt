package ai.moneymanager

import ai.moneymanager.chat.config.GeminiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties::class)
class MoneyManagerAIApplication

fun main(args: Array<String>) {
    runApplication<MoneyManagerAIApplication>(*args)
}
