package ai.moneymanager.mapper

import com.fasterxml.jackson.databind.ObjectMapper

class GeminiArgsMapper(
    val objectMapper: ObjectMapper
) {
    inline fun <reified T : Any> map(args: Map<String, Any>): T {
        return objectMapper.convertValue(args, T::class.java)
    }
}
