package ai.moneymanager.service

import ai.moneymanager.domain.model.TelegramProfile
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.UserInfoEntity
import ai.moneymanager.web.security.TEST_BOT_TOKEN
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

private const val RECYCLED_USERNAME = "amir"
private const val FORMER_OWNER_ID = 1L
private const val NEW_OWNER_ID = 2L

class UserInfoServiceTest {
    private val repository = FakeUserInfoRepository()
    private val adminNotificationService = mock(AdminNotificationService::class.java)
    private val service = UserInfoService(repository, adminNotificationService, TEST_BOT_TOKEN)

    @Test
    fun `returns existing user found by telegram id`() {
        val existing = repository.givenUser(telegramUserId = FORMER_OWNER_ID, username = RECYCLED_USERNAME)

        val user = service.getOrCreate(profile(FORMER_OWNER_ID, username = RECYCLED_USERNAME))

        assertThat(user.id).isEqualTo(existing.id)
        assertThat(notifiedUsers()).isEmpty()
    }

    @Test
    fun `does not hand over an account to the new owner of a recycled username`() {
        val formerOwner = repository.givenUser(telegramUserId = FORMER_OWNER_ID, username = RECYCLED_USERNAME)

        val user = service.getOrCreate(profile(NEW_OWNER_ID, username = RECYCLED_USERNAME))

        assertThat(user.id).isNotEqualTo(formerOwner.id)
        assertThat(user.telegramUserId).isEqualTo(NEW_OWNER_ID)
    }

    @Test
    fun `keeps the same account after the user changes username`() {
        val existing = repository.givenUser(telegramUserId = FORMER_OWNER_ID, username = RECYCLED_USERNAME)

        val user = service.getOrCreate(profile(FORMER_OWNER_ID, username = "amir_new"))

        assertThat(user.id).isEqualTo(existing.id)
        assertThat(repository.users).hasSize(1)
    }

    @Test
    fun `creates first-time user and notifies admin`() {
        val user = service.getOrCreate(profile(NEW_OWNER_ID, username = null))

        assertThat(user.telegramUserId).isEqualTo(NEW_OWNER_ID)
        assertThat(user.firstName).isEqualTo("Amir")
        assertThat(repository.users).hasSize(1)
        assertThat(notifiedUsers().map { it.telegramUserId }).containsExactly(NEW_OWNER_ID)
    }

    private fun notifiedUsers(): List<UserInfo> =
        mockingDetails(adminNotificationService).invocations.map { it.arguments.single() as UserInfo }

    private fun profile(telegramUserId: Long, username: String?) = TelegramProfile(
        telegramUserId = telegramUserId,
        username = username,
        firstName = "Amir",
        lastName = null,
        languageCode = "ru",
    )
}

private class FakeUserInfoRepository(
    private val delegate: UserInfoRepository = mock(UserInfoRepository::class.java)
) : UserInfoRepository by delegate {

    val users = mutableListOf<UserInfoEntity>()

    fun givenUser(telegramUserId: Long, username: String?): UserInfoEntity =
        save(
            UserInfoEntity(
                username = username,
                firstName = "Amir",
                lastName = null,
                telegramUserId = telegramUserId,
                languageCode = "ru",
            )
        )

    override fun findUserInfoEntityByTelegramUserId(id: Long): UserInfoEntity? =
        users.firstOrNull { it.telegramUserId == id }

    override fun findUserInfoEntityByUsername(username: String): UserInfoEntity? =
        users.firstOrNull { it.username == username }

    @Suppress("UNCHECKED_CAST")
    override fun <S : UserInfoEntity> save(entity: S): S {
        val stored = entity.copy(id = entity.id ?: ObjectId())
        users.removeIf { it.id == stored.id }
        users.add(stored)
        return stored as S
    }
}
