package ai.moneymanager.web.error

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.web.security.CurrentUserArgumentResolver
import ai.moneymanager.web.security.TelegramPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

private const val WEB_BASE_PACKAGE = "ai.moneymanager.web"

private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

@RestControllerAdvice(basePackages = [WEB_BASE_PACKAGE])
class ApiExceptionHandler(
    private val errorFactory: ApiErrorFactory,
    private val localizationService: LocalizationService,
) {
    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException, request: HttpServletRequest): ResponseEntity<ApiError> =
        respond(exception, request)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class)
    fun handleMalformedRequest(request: HttpServletRequest): ResponseEntity<ApiError> =
        respond(ApiException(ApiErrorCode.BAD_REQUEST), request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unhandled API error on {} {}", request.method, request.requestURI, exception)
        return respond(ApiException(ApiErrorCode.INTERNAL_ERROR), request)
    }

    private fun respond(exception: ApiException, request: HttpServletRequest): ResponseEntity<ApiError> =
        ResponseEntity
            .status(exception.code.status)
            .body(errorFactory.create(exception, languageOf(request)))

    private fun languageOf(request: HttpServletRequest): String {
        val user = request.getAttribute(CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE) as? UserInfo
        val principal = request.getAttribute(TelegramPrincipal.REQUEST_ATTRIBUTE) as? TelegramPrincipal
        return localizationService.resolveLanguage(user?.language, principal?.languageCode)
    }
}
