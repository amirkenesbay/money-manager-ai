package ai.moneymanager.service

import ai.moneymanager.service.fx.GroupCurrencyConverter
import ai.moneymanager.domain.model.GroupCreationResult
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.MoneyGroupEntity
import ai.moneymanager.repository.entity.UserInfoEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

private const val OWNER_ID = 7L

class GroupServiceCreationTest {
    private val groupId = ObjectId()
    private val groupRepository = mock(MoneyGroupRepository::class.java)
    private val userRepository = mock(UserInfoRepository::class.java)
    private val categoryService = mock(CategoryService::class.java)
    private val service = GroupService(groupRepository, userRepository, categoryService, mock(GroupCurrencyConverter::class.java)) {}

    @BeforeEach
    fun givenRepositoriesThatSave() {
        `when`(groupRepository.save(any(MoneyGroupEntity::class.java))).thenAnswer { it.getArgument<MoneyGroupEntity>(0).copy(id = groupId) }
        `when`(userRepository.save(any(UserInfoEntity::class.java))).thenAnswer { it.getArgument<UserInfoEntity>(0) }
    }

    @Test
    fun `a new shared group starts with default categories in the owner's language`() {
        `when`(userRepository.findFirstByTelegramUserIdOrderByIdAsc(OWNER_ID)).thenReturn(owner(language = "en"))

        val result = service.createGroup(OWNER_ID, "Family")

        assertThat(result).isInstanceOf(GroupCreationResult.Created::class.java)
        verify(categoryService).createDefaultCategories(groupId, "en")
    }

    @Test
    fun `falls back to the default language when the owner has not chosen one`() {
        `when`(userRepository.findFirstByTelegramUserIdOrderByIdAsc(OWNER_ID)).thenReturn(owner(language = null))

        service.createGroup(OWNER_ID, "Семья")

        verify(categoryService).createDefaultCategories(groupId, LocalizationService.FALLBACK_LANGUAGE)
    }

    private fun owner(language: String?) =
        UserInfoEntity(username = null, firstName = "Owner", lastName = null, telegramUserId = OWNER_ID, languageCode = null, language = language)
}
