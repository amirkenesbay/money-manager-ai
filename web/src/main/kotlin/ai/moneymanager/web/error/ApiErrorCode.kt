package ai.moneymanager.web.error

import org.springframework.http.HttpStatus

const val API_ERROR_KEY_PREFIX = "api.error."

enum class ApiErrorCode(val status: HttpStatus) {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    PREMIUM_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT),
    CATEGORY_DUPLICATE(HttpStatus.CONFLICT),
    CATEGORY_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_CONTENT),
    GROUP_DUPLICATE(HttpStatus.CONFLICT),
    GROUP_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_CONTENT),
    NOTIFICATION_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_CONTENT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    val messageKey: String get() = API_ERROR_KEY_PREFIX + name.lowercase()
}
