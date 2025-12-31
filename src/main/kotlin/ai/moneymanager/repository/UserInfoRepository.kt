package ai.moneymanager.repository

import ai.moneymanager.repository.entity.UserInfoEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserInfoRepository : MongoRepository<UserInfoEntity, ObjectId> {
    fun findUserInfoEntityByUsername(username: String): UserInfoEntity?
    fun findUserInfoEntityByTelegramUserId(id: Long): UserInfoEntity?
}