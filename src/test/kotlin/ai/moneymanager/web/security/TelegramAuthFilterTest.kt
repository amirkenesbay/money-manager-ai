package ai.moneymanager.web.security

import ai.moneymanager.web.error.ApiError
import ai.moneymanager.web.error.ApiErrorCode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class TelegramAuthFilterTest {
    private val objectMapper = jacksonObjectMapper()
    private val filter = TelegramAuthFilter(
        TelegramInitDataValidator(TEST_BOT_TOKEN, objectMapper),
        objectMapper,
    )

    private val request = MockHttpServletRequest("GET", "/api/v1/me")
    private val response = MockHttpServletResponse()
    private val chain = RecordingFilterChain()

    @Test
    fun `passes request through and exposes principal when initData is valid`() {
        withTmaAuthorization(TestInitData.valid(userId = 42L))

        filter.doFilter(request, response, chain)

        assertThat(chain.invoked).isTrue()
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        val principal = request.getAttribute(TelegramPrincipal.REQUEST_ATTRIBUTE) as TelegramPrincipal
        assertThat(principal.userId).isEqualTo(42L)
    }

    @Test
    fun `rejects expired initData`() {
        withTmaAuthorization(TestInitData.valid(authDate = TestInitData.secondsAgo(100_000)))

        filter.doFilter(request, response, chain)

        assertUnauthorized()
    }

    @Test
    fun `rejects tampered signature`() {
        withTmaAuthorization(TestInitData.tamperHash(TestInitData.valid()))

        filter.doFilter(request, response, chain)

        assertUnauthorized()
    }

    @Test
    fun `rejects initData signed with a foreign bot token`() {
        withTmaAuthorization(TestInitData.valid(botToken = "999999:SOMEONE-ELSES-TOKEN"))

        filter.doFilter(request, response, chain)

        assertUnauthorized()
    }

    @Test
    fun `rejects request without authorization header`() {
        filter.doFilter(request, response, chain)

        assertUnauthorized()
    }

    @Test
    fun `rejects authorization header without the tma scheme`() {
        withAuthorization("Bearer ${TestInitData.valid()}")

        filter.doFilter(request, response, chain)

        assertUnauthorized()
    }

    @Test
    fun `reports unauthorized in the shared api error format`() {
        filter.doFilter(request, response, chain)

        assertThat(response.contentType).startsWith(MediaType.APPLICATION_JSON_VALUE)
        val error = objectMapper.readValue(response.contentAsByteArray, ApiError::class.java)
        assertThat(error.code).isEqualTo(ApiErrorCode.UNAUTHORIZED)
        assertThat(error.message).isNotBlank()
    }

    private fun withTmaAuthorization(initData: String) {
        withAuthorization("tma $initData")
    }

    private fun withAuthorization(value: String) {
        request.addHeader(HttpHeaders.AUTHORIZATION, value)
    }

    private fun assertUnauthorized() {
        assertThat(chain.invoked).isFalse()
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(request.getAttribute(TelegramPrincipal.REQUEST_ATTRIBUTE)).isNull()
    }
}

private class RecordingFilterChain : FilterChain {
    var invoked = false
        private set

    override fun doFilter(request: ServletRequest, response: ServletResponse) {
        invoked = true
    }
}
