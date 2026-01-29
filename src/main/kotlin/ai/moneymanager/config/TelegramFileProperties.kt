package ai.moneymanager.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "telegram.file")
class TelegramFileProperties {
    var maxVoiceDurationSeconds: Int = 180
    var maxFileSizeBytes: Int = 2 * 1024 * 1024
    var apiUrl: String = "https://api.telegram.org/file/bot"
}