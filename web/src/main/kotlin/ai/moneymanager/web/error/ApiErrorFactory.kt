package ai.moneymanager.web.error

import ai.moneymanager.service.LocalizationService
import org.springframework.stereotype.Component

@Component
class ApiErrorFactory(
    private val localizationService: LocalizationService,
) {
    fun create(exception: ApiException, language: String?): ApiError = ApiError(
        code = exception.code,
        message = localizationService.t(exception.messageKey, language, *exception.messageArgs.toTypedArray()),
        details = exception.details,
    )
}
