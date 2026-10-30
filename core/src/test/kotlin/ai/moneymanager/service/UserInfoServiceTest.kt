package ai.moneymanager.service

import ai.moneymanager.domain.event.NewUserRegistered
import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.domain.model.TelegramProfile
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.UserInfoEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.dao.DuplicateKeyException

private const val RECYCLED_USERNAME = "amir"
private const val FORMER_OWNER_ID = 1L
private const val NEW_OWNER_ID = 2L

class UserInfoServiceTest {
    private val repository = FakeUserInfoRepository()
    private val publishedEvents = mutableListOf<Any>()
    private val service = UserInfoService(repository) { publishedEvents.add(it) }

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

    @Test
    fun `returns the user created by a concurrent request instead of a duplicate`() {
        repository.loseRaceTo(NEW_OWNER_ID)

        val user = service.getOrCreate(profile(NEW_OWNER_ID, username = null))

        assertThat(repository.users).hasSize(1)
        assertThat(user.id).isEqualTo(repository.users.single().id)
        assertThat(notifiedUsers()).isEmpty()
    }

    @Test
    fun `remembers each shown hint once`() {
        repository.givenUser(telegramUserId = FORMER_OWNER_ID, username = RECYCLED_USERNAME)

        service.markHintSeen(FORMER_OWNER_ID, AppHint.WELCOME)
        val user = service.markHintSeen(FORMER_OWNER_ID, AppHint.WELCOME)

        assertThat(user?.seenHints).containsExactly(AppHint.WELCOME)
        assertThat(repository.users.single().seenHints).containsExactly(AppHint.WELCOME)
    }

    @Test
    fun `names known users by full name and unknown ones by id`() {
        repository.givenUser(telegramUserId = FORMER_OWNER_ID, username = RECYCLED_USERNAME)

        val names = service.displayNames(setOf(FORMER_OWNER_ID, NEW_OWNER_ID))

        assertThat(names).containsEntry(FORMER_OWNER_ID, "Amir")
        assertThat(names.getValue(NEW_OWNER_ID)).contains(NEW_OWNER_ID.toString())
    }

    private fun notifiedUsers(): List<UserInfo> =
        publishedEvents.filterIsInstance<NewUserRegistered>().map { it.user }

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
    private var concurrentlyInsertedUser: UserInfoEntity? = null

    fun loseRaceTo(telegramUserId: Long) {
        concurrentlyInsertedUser = newUser(telegramUserId, username = null)
    }

    fun givenUser(telegramUserId: Long, username: String?): UserInfoEntity = save(newUser(telegramUserId, username))

    private fun newUser(telegramUserId: Long, username: String?) = UserInfoEntity(
        username = username,
        firstName = "Amir",
        lastName = null,
        telegramUserId = telegramUserId,
        languageCode = "ru",
    )

    override fun findFirstByTelegramUserIdOrderByIdAsc(id: Long): UserInfoEntity? =
        users.firstOrNull { it.telegramUserId == id }

    override fun findFirstByUsernameOrderByIdAsc(username: String): UserInfoEntity? =
        users.firstOrNull { it.username == username }

    @Suppress("UNCHECKED_CAST")
    override fun <S : UserInfoEntity> save(entity: S): S {
        concurrentlyInsertedUser?.let { winner ->
            concurrentlyInsertedUser = null
            users.add(winner.copy(id = ObjectId()))
            throw DuplicateKeyException("duplicate telegramUserId ${winner.telegramUserId}")
        }
        val stored = entity.copy(id = entity.id ?: ObjectId())
        users.removeIf { it.id == stored.id }
        users.add(stored)
        return stored as S
    }
}
