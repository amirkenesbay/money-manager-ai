package ai.moneymanager.web.security

import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiErrorFactory
import ai.moneymanager.web.error.ApiException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper

private const val AUTH_SCHEME_PREFIX = "tma "

class TelegramAuthFilter(
    private val validator: TelegramInitDataValidator,
    private val errorFactory: ApiErrorFactory,
    private val jsonMapper: JsonMapper,
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
        val exception = ApiException(ApiErrorCode.UNAUTHORIZED)
        val error = errorFactory.create(exception, language = null)
        response.status = exception.code.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        jsonMapper.writeValue(response.outputStream, error)
    }
}
