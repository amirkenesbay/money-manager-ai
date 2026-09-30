package ai.moneymanager.web.error

data class ApiError(
    val code: ApiErrorCode,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
)
