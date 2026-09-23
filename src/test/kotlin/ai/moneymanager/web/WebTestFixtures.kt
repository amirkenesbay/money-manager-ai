package ai.moneymanager.web

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.web.json.ApiJsonConfig
import ai.moneymanager.web.security.TEST_FIRST_NAME
import ai.moneymanager.web.security.TEST_LANGUAGE_CODE
import ai.moneymanager.web.security.TEST_USER_ID
import ai.moneymanager.web.security.TestInitData
import org.bson.types.ObjectId
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

private const val MESSAGES_BASENAME = "messages"
private const val TEST_INVITE_TOKEN = "abc123xyz"
private const val TEST_GROUP_NAME = "Семья"

const val ENGLISH = "en"

fun testLocalizationService(): LocalizationService = LocalizationService(
    ResourceBundleMessageSource().apply {
        setBasename(MESSAGES_BASENAME)
        setDefaultEncoding(Charsets.UTF_8.name())
        setFallbackToSystemLocale(false)
    }
)

fun testJsonMapper(): JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModule(ApiJsonConfig().apiJsonModule())
    .build()

fun MockHttpServletRequestBuilder.withTelegramAuth(userId: Long = TEST_USER_ID): MockHttpServletRequestBuilder =
    header(HttpHeaders.AUTHORIZATION, "tma ${TestInitData.valid(userId = userId)}")

fun testUser(
    language: String? = null,
    activeGroupId: ObjectId? = null,
) = UserInfo(
    id = ObjectId(),
    username = null,
    firstName = TEST_FIRST_NAME,
    lastName = null,
    telegramUserId = TEST_USER_ID,
    languageCode = TEST_LANGUAGE_CODE,
    language = language,
    activeGroupId = activeGroupId,
)

fun testGroup(
    id: ObjectId? = ObjectId(),
    members: Set<Long> = setOf(TEST_USER_ID),
    type: GroupType = GroupType.SHARED,
) = MoneyGroup(
    id = id,
    name = TEST_GROUP_NAME,
    inviteToken = TEST_INVITE_TOKEN,
    ownerId = members.first(),
    memberIds = members,
    type = type,
)
