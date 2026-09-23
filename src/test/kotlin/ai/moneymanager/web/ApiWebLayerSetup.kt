package ai.moneymanager.web

import ai.moneymanager.service.LocalizationService
import ai.moneymanager.web.config.WebSecurityConfig
import ai.moneymanager.web.error.ApiErrorFactory
import ai.moneymanager.web.json.ApiJsonConfig
import ai.moneymanager.web.security.TEST_BOT_TOKEN
import ai.moneymanager.web.security.TelegramInitDataValidator
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
    WebSecurityConfig::class,
    ApiJsonConfig::class,
    ApiErrorFactory::class,
    TelegramInitDataValidator::class,
    LocalizationService::class,
)
@TestPropertySource(properties = ["chat-machinist.bot.token=$TEST_BOT_TOKEN"])
annotation class ApiWebLayerSetup
