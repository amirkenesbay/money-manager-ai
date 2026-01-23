package ai.moneymanager.mapper

import com.fasterxml.jackson.databind.ObjectMapper

class GeminiArgsMapper(
    val objectMapper: ObjectMapper
) {
    inline fun <reified T : Any> map(args: Map<String, Any>): T {
        // convertValue безопаснее/чище чем “через строковый JSON”
        return objectMapper.convertValue(args, T::class.java)
    }
}