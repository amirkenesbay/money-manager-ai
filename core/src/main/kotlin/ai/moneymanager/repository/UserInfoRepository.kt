package ai.moneymanager.repository

import ai.moneymanager.domain.model.UserAccess
import ai.moneymanager.repository.entity.UserInfoEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserInfoRepository : MongoRepository<UserInfoEntity, ObjectId>, UserInfoRepositoryCustom {
    fun findFirstByUsernameOrderByIdAsc(username: String): UserInfoEntity?
    fun findFirstByTelegramUserIdOrderByIdAsc(id: Long): UserInfoEntity?
    fun findByActiveGroupIdIsNotNullAndTelegramUserIdIsNotNull(): List<UserInfoEntity>
    fun findByAccess(access: UserAccess): List<UserInfoEntity>
    fun findByDailyReminderSeededNotAndTelegramUserIdIsNotNull(seeded: Boolean): List<UserInfoEntity>
}
