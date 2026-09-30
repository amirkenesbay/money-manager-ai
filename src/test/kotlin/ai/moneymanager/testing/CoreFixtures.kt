package ai.moneymanager.testing

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.LocalizationService
import org.bson.types.ObjectId
import org.springframework.context.support.ResourceBundleMessageSource

private const val MESSAGES_BASENAME = "messages"
private const val TEST_INVITE_TOKEN = "abc123xyz"
private const val TEST_GROUP_NAME = "Семья"

const val TEST_USER_ID = 42L
const val TEST_FIRST_NAME = "Amir"
const val TEST_LANGUAGE_CODE = "ru"
const val ENGLISH = "en"

fun testLocalizationService(): LocalizationService = LocalizationService(
    ResourceBundleMessageSource().apply {
        setBasename(MESSAGES_BASENAME)
        setDefaultEncoding(Charsets.UTF_8.name())
        setFallbackToSystemLocale(false)
    }
)

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
