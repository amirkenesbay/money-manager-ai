package ai.moneymanager.service

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class LocalizationService(
    private val messageSource: MessageSource
) {
    fun t(key: String, language: String?, vararg args: Any): String {
        val locale = language?.let { Locale.of(it) } ?: DEFAULT_LOCALE
        return messageSource.getMessage(key, args, locale)
    }

    fun resolveLanguage(selected: String?, telegramLanguageCode: String?): String {
        if (selected != null && selected in SUPPORTED_LANGUAGES) return selected
        if (telegramLanguageCode != null && telegramLanguageCode in SUPPORTED_LANGUAGES) return telegramLanguageCode
        return FALLBACK_LANGUAGE
    }

    companion object {
        const val FALLBACK_LANGUAGE = "ru"
        val SUPPORTED_LANGUAGES = setOf("ru", "en", "kk")
        private val DEFAULT_LOCALE = Locale.of(FALLBACK_LANGUAGE)
    }
}
