package ai.moneymanager.web.config

import ai.moneymanager.web.error.ApiErrorFactory
import ai.moneymanager.web.security.TelegramAuthFilter
import ai.moneymanager.web.security.TelegramInitDataValidator
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

private const val API_URL_PATTERN = "/api/*"

@Configuration
class WebSecurityConfig {
    @Bean
    fun telegramAuthFilterRegistration(
        validator: TelegramInitDataValidator,
        errorFactory: ApiErrorFactory,
        jsonMapper: JsonMapper,
    ): FilterRegistrationBean<TelegramAuthFilter> =
        FilterRegistrationBean(TelegramAuthFilter(validator, errorFactory, jsonMapper)).apply {
            addUrlPatterns(API_URL_PATTERN)
        }
}
