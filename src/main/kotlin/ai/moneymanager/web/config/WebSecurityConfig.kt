package ai.moneymanager.web.config

import ai.moneymanager.web.security.TelegramAuthFilter
import ai.moneymanager.web.security.TelegramInitDataValidator
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val API_URL_PATTERN = "/api/*"

@Configuration
class WebSecurityConfig {
    @Bean
    fun telegramAuthFilterRegistration(
        validator: TelegramInitDataValidator,
        objectMapper: ObjectMapper,
    ): FilterRegistrationBean<TelegramAuthFilter> =
        FilterRegistrationBean(TelegramAuthFilter(validator, objectMapper)).apply {
            addUrlPatterns(API_URL_PATTERN)
        }
}
