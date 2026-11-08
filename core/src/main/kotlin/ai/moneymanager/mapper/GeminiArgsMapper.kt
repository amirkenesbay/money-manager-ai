package ai.moneymanager.mapper

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

class GeminiArgsMapper(
    val objectMapper: ObjectMapper
) {
    @PublishedApi
    internal val lenientMapper: ObjectMapper = objectMapper.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    inline fun <reified T : Any> map(args: Map<String, Any>): T {
        return lenientMapper.convertValue(args, T::class.java)
    }
}
