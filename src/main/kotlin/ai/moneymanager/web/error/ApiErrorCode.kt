package ai.moneymanager.web.error

import org.springframework.http.HttpStatus

const val API_ERROR_KEY_PREFIX = "api.error."

enum class ApiErrorCode(val status: HttpStatus) {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    val messageKey: String get() = API_ERROR_KEY_PREFIX + name.lowercase()
}
