package ai.moneymanager.repository

import ai.moneymanager.repository.entity.UserInfoEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserInfoRepository : MongoRepository<UserInfoEntity, ObjectId> {
    fun findFirstByUsernameOrderByIdAsc(username: String): UserInfoEntity?
    fun findFirstByTelegramUserIdOrderByIdAsc(id: Long): UserInfoEntity?
}
