package ai.moneymanager.web.me

import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.http.HttpStatus
import ai.moneymanager.metrics.MessageType
import ai.moneymanager.metrics.MessageMetrics
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.OnboardingService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.badRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId

private const val UNSUPPORTED_LANGUAGE_KEY = "${API_ERROR_KEY_PREFIX}unsupported_language"
private const val SUPPORTED_LANGUAGES_DETAIL = "supported"
private const val UNSUPPORTED_TIMEZONE_KEY = "${API_ERROR_KEY_PREFIX}unsupported_timezone"

@RestController
@RequestMapping("$API_V1_PREFIX/me")
class MeController(
    private val onboardingService: OnboardingService,
    private val userInfoService: UserInfoService,
    private val meResponseFactory: MeResponseFactory,
    private val messageMetrics: MessageMetrics,
) {
    @GetMapping
    fun me(user: UserInfo): MeResponse = meResponseFactory.create(user)

    @PatchMapping
    fun update(user: UserInfo, @RequestBody request: UpdateMeRequest): MeResponse {
        val userId = checkNotNull(user.telegramUserId)
        if (request.language == null && request.timezone == null && request.recurringHeadsUp == null) throw ApiException(ApiErrorCode.BAD_REQUEST)
        request.language?.let { requireSupported(it) }
        request.timezone?.let { requireZone(it) }
        val withLanguage = request.language?.let { found(onboardingService.selectLanguage(userId, it)) } ?: user
        val withTimezone = request.timezone?.let { found(userInfoService.updateTimezone(userId, it)) } ?: withLanguage
        val updated = request.recurringHeadsUp?.let { found(userInfoService.setRecurringHeadsUp(userId, it)) } ?: withTimezone
        return meResponseFactory.create(updated)
    }

    @PostMapping("/hints")
    fun markHint(user: UserInfo, @RequestBody request: MarkHintRequest): MeResponse =
        meResponseFactory.create(found(userInfoService.markHintSeen(checkNotNull(user.telegramUserId), request.hint)))

    @PostMapping("/digests")
    fun setDigest(user: UserInfo, @RequestBody request: DigestSettingRequest): MeResponse =
        meResponseFactory.create(found(userInfoService.setDigestMuted(checkNotNull(user.telegramUserId), request.kind, request.muted)))

    @PostMapping("/opened")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun opened(user: UserInfo, @RequestBody request: OpenedFromRequest) {
        MessageType.of(request.source)?.let(messageMetrics::opened)
    }

    private fun found(user: UserInfo?): UserInfo = user ?: throw ApiException(ApiErrorCode.NOT_FOUND)

    private fun requireZone(timezone: String) {
        if (runCatching { ZoneId.of(timezone) }.isFailure) throw badRequest(UNSUPPORTED_TIMEZONE_KEY)
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
