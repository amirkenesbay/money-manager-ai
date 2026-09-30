package ai.moneymanager.web

import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.web.json.ApiJsonConfig
import ai.moneymanager.web.security.TestInitData
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

fun testJsonMapper(): JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModule(ApiJsonConfig().apiJsonModule())
    .build()

fun MockHttpServletRequestBuilder.withTelegramAuth(userId: Long = TEST_USER_ID): MockHttpServletRequestBuilder =
    header(HttpHeaders.AUTHORIZATION, "tma ${TestInitData.valid(userId = userId)}")
