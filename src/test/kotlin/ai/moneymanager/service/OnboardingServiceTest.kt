package ai.moneymanager.service

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.web.ENGLISH
import ai.moneymanager.web.testGroup
import ai.moneymanager.web.security.TEST_USER_ID
import ai.moneymanager.web.testLocalizationService
import ai.moneymanager.web.testUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

private const val CREATE_PERSONAL_GROUP = "createPersonalGroup"

class OnboardingServiceTest {
    private val userInfoService = mock(UserInfoService::class.java)
    private val groupService = mock(GroupService::class.java)
    private val localizationService = testLocalizationService()
    private val service = OnboardingService(userInfoService, groupService, localizationService)

    @Test
    fun `creates localized personal group when user has none`() {
        `when`(userInfoService.updateLanguage(TEST_USER_ID, ENGLISH)).thenReturn(testUser(language = ENGLISH))
        `when`(groupService.getUserGroups(TEST_USER_ID)).thenReturn(emptyList())
        `when`(userInfoService.getUserInfoByTelegramId(TEST_USER_ID)).thenReturn(testUser(language = ENGLISH))

        val user = service.selectLanguage(TEST_USER_ID, ENGLISH)

        assertThat(user?.language).isEqualTo(ENGLISH)
        val creation = personalGroupCreations().single()
        assertThat(creation[0]).isEqualTo(TEST_USER_ID)
        assertThat(creation[1]).isEqualTo(localizationService.t(PERSONAL_GROUP_NAME_KEY, ENGLISH))
        assertThat(creation[2]).isEqualTo(ENGLISH)
    }

    @Test
    fun `keeps existing groups untouched`() {
        `when`(userInfoService.updateLanguage(TEST_USER_ID, ENGLISH)).thenReturn(testUser(language = ENGLISH))
        `when`(groupService.getUserGroups(TEST_USER_ID)).thenReturn(listOf(testGroup(type = GroupType.PERSONAL)))
        `when`(userInfoService.getUserInfoByTelegramId(TEST_USER_ID)).thenReturn(testUser(language = ENGLISH))

        service.selectLanguage(TEST_USER_ID, ENGLISH)

        assertThat(personalGroupCreations()).isEmpty()
    }

    @Test
    fun `does nothing for unknown user`() {
        `when`(userInfoService.updateLanguage(TEST_USER_ID, ENGLISH)).thenReturn(null)

        assertThat(service.selectLanguage(TEST_USER_ID, ENGLISH)).isNull()
        assertThat(personalGroupCreations()).isEmpty()
    }

    private fun personalGroupCreations(): List<Array<Any?>> =
        mockingDetails(groupService).invocations
            .filter { it.method.name == CREATE_PERSONAL_GROUP }
            .map { it.arguments }
}
