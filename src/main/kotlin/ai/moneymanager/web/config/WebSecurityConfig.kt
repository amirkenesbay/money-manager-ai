package ai.moneymanager.web.config

import ai.moneymanager.web.security.TelegramAuthFilter
import ai.moneymanager.web.security.TelegramInitDataValidator
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Everything under the REST API is authenticated; the bot's own webhook traffic is not. */
private const val API_URL_PATTERN = "/api/*"

@Configuration
class WebSecurityConfig {

    /**
     * Registers the filter explicitly instead of annotating it `@Component`, because a filter
     * bean is otherwise mapped to every path — and authentication must cover the API alone.
     */
    @Bean
    fun telegramAuthFilterRegistration(
        validator: TelegramInitDataValidator,
        objectMapper: ObjectMapper,
    ): FilterRegistrationBean<TelegramAuthFilter> =
        FilterRegistrationBean(TelegramAuthFilter(validator, objectMapper)).apply {
            addUrlPatterns(API_URL_PATTERN)
        }
}
