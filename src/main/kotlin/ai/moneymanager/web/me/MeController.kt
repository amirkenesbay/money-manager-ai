package ai.moneymanager.web.me

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.OnboardingService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val UNSUPPORTED_LANGUAGE_KEY = "${API_ERROR_KEY_PREFIX}unsupported_language"
private const val SUPPORTED_LANGUAGES_DETAIL = "supported"

@RestController
@RequestMapping("$API_V1_PREFIX/me")
class MeController(
    private val onboardingService: OnboardingService,
    private val meResponseFactory: MeResponseFactory,
) {
    @GetMapping
    fun me(user: UserInfo): MeResponse = meResponseFactory.create(user)

    @PatchMapping
    fun update(user: UserInfo, @RequestBody request: UpdateMeRequest): MeResponse {
        requireSupported(request.language)
        val updated = onboardingService.selectLanguage(checkNotNull(user.telegramUserId), request.language)
            ?: throw ApiException(ApiErrorCode.NOT_FOUND)
        return meResponseFactory.create(updated)
    }

    private fun requireSupported(language: String) {
        if (language in LocalizationService.SUPPORTED_LANGUAGES) return
        throw ApiException(
            code = ApiErrorCode.BAD_REQUEST,
            messageKey = UNSUPPORTED_LANGUAGE_KEY,
            details = mapOf(SUPPORTED_LANGUAGES_DETAIL to LocalizationService.SUPPORTED_LANGUAGES.sorted()),
        )
    }
}
