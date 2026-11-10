package ai.moneymanager.domain.model.nlp

import ai.moneymanager.domain.model.nlp.enum.GeminiFunction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeminiFunctionTest {
    @Test
    fun `every function matches a declared signature`() {
        GeminiFunction.entries.forEach { function ->
            assertThat(function.method.name).isEqualTo(function.functionName)
        }
    }
}
