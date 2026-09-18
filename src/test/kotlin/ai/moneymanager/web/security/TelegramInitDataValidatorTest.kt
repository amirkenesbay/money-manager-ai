package ai.moneymanager.web.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TelegramInitDataValidatorTest {

    private val validator = TelegramInitDataValidator(TEST_BOT_TOKEN, ObjectMapper())

    @Test
    fun `returns principal for correctly signed initData`() {
        val principal = requireNotNull(validator.validate(TestInitData.valid())) {
            "expected correctly signed initData to be accepted"
        }

        assertThat(principal.userId).isEqualTo(42L)
        assertThat(principal.firstName).isEqualTo("Amir")
        assertThat(principal.languageCode).isEqualTo("ru")
        assertThat(principal.lastName).isNull()
    }

    @Test
    fun `returns null when hash is tampered`() {
        val tampered = TestInitData.tamperHash(TestInitData.valid())

        assertThat(validator.validate(tampered)).isNull()
    }

    @Test
    fun `returns null when hash is missing`() {
        assertThat(validator.validate("auth_date=${TestInitData.currentTimestamp()}&user=%7B%7D")).isNull()
    }

    @Test
    fun `returns null when auth_date is too old`() {
        val stale = TestInitData.valid(authDate = TestInitData.secondsAgo(100_000))

        assertThat(validator.validate(stale)).isNull()
    }

    @Test
    fun `ignores signature field when computing hash`() {
        val initData = TestInitData.signed(
            params = mapOf(
                "auth_date" to TestInitData.currentTimestamp(),
                "user" to """{"id":42,"first_name":"Amir"}""",
            ),
            extraUnsignedParams = mapOf("signature" to "ed25519-signature-not-checked"),
        )

        assertThat(validator.validate(initData)).isNotNull
    }
}
