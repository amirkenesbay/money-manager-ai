package ai.moneymanager.web

import ai.moneymanager.service.LocalizationService
import ai.moneymanager.web.json.ApiJsonConfig
import org.springframework.context.support.ResourceBundleMessageSource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

private const val MESSAGES_BASENAME = "messages"

fun testLocalizationService(): LocalizationService = LocalizationService(
    ResourceBundleMessageSource().apply {
        setBasename(MESSAGES_BASENAME)
        setDefaultEncoding(Charsets.UTF_8.name())
        setFallbackToSystemLocale(false)
    }
)

fun testJsonMapper(): JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModule(ApiJsonConfig().apiJsonModule())
    .build()
