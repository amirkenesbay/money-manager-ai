package ai.moneymanager.web.subscription

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val RFC_KEY = "key"
private const val RFC_MESSAGE = "The quick brown fox jumps over the lazy dog"
private const val RFC_HMAC = "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8"

class TributeSignatureTest {
    private val signature = TributeSignature(RFC_KEY)

    @Test
    fun `signs the body as lowercase hex hmac sha256`() {
        assertThat(signature.sign(RFC_MESSAGE.toByteArray())).isEqualTo(RFC_HMAC)
    }

    @Test
    fun `accepts the right signature in any case and rejects others`() {
        assertThat(signature.matches(RFC_MESSAGE.toByteArray(), RFC_HMAC.uppercase())).isTrue()
        assertThat(signature.matches(RFC_MESSAGE.toByteArray(), RFC_HMAC.replaceFirst('f', '0'))).isFalse()
        assertThat(signature.matches(RFC_MESSAGE.toByteArray(), null)).isFalse()
    }
}
