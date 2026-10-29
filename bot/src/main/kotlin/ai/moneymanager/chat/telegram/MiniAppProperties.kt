package ai.moneymanager.chat.telegram

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "mini-app")
class MiniAppProperties {
    var url: String = ""
    var botGateway: Boolean = false
}
