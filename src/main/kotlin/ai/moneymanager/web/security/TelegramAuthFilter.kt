package ai.moneymanager.web.security

import ai.moneymanager.web.error.ApiError
import ai.moneymanager.web.error.ApiErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

/** Authorization scheme Telegram Mini Apps use to carry `initData`. */
private const val AUTH_SCHEME_PREFIX = "tma "

/**
 * Deliberately vague: the client is not told whether the signature was forged, the data
 * expired, or the header was absent — that distinction only helps someone probing the API.
 */
private const val UNAUTHORIZED_MESSAGE = "Valid Telegram initData is required"

/**
 * Authenticates every request under `/api` by the `initData` string Telegram hands to the Mini App.
 *
 * Expects `Authorization: tma <initData>`. On success the resolved [TelegramPrincipal] is put
 * into the request under [TelegramPrincipal.REQUEST_ATTRIBUTE] for controllers to read;
 * otherwise the chain is cut short with `401` and the error format of `docs/webapp/03-API.md`.
 *
 * The filter writes that body itself because servlet filters run before the dispatcher, and so
 * outside the reach of the `@ControllerAdvice` handler that formats every other API error.
 */
class TelegramAuthFilter(
    private val validator: TelegramInitDataValidator,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val principal = authenticate(request)
        if (principal == null) {
            writeUnauthorized(response)
            return
        }
        request.setAttribute(TelegramPrincipal.REQUEST_ATTRIBUTE, principal)
        filterChain.doFilter(request, response)
    }

    private fun authenticate(request: HttpServletRequest): TelegramPrincipal? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!header.startsWith(AUTH_SCHEME_PREFIX)) return null
        return validator.validate(header.removePrefix(AUTH_SCHEME_PREFIX))
    }

    private fun writeUnauthorized(response: HttpServletResponse) {
        val error = ApiError(code = ApiErrorCode.UNAUTHORIZED, message = UNAUTHORIZED_MESSAGE)
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.outputStream, error)
    }
}
