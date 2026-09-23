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

private const val AUTH_SCHEME_PREFIX = "tma "

private const val UNAUTHORIZED_MESSAGE = "Valid Telegram initData is required"

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
