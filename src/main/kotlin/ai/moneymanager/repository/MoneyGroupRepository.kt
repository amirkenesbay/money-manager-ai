package ai.moneymanager.repository

import ai.moneymanager.repository.entity.MoneyGroupEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MoneyGroupRepository : MongoRepository<MoneyGroupEntity, ObjectId> {
    fun findByInviteToken(inviteToken: String): MoneyGroupEntity?
    fun findByIdIn(ids: Set<ObjectId>): List<MoneyGroupEntity>
    fun findByMemberTelegramUserIdsContaining(memberId: Long): List<MoneyGroupEntity>

    @Query("{ '_id': ?0, '\$or': [ { 'ownerTelegramUserId': ?1 }, { 'memberTelegramUserIds': ?1 } ] }", exists = true)
    fun existsByIdAndOwnerOrMemberTelegramUserId(groupId: ObjectId?, telegramUserId: Long): Boolean

    @Query(value = "{ '_id': ?0, 'ownerTelegramUserId': ?1 }", exists = true)
    fun existsByIdAndOwnerTelegramUserId(groupId: ObjectId, telegramUserId: Long): Boolean
}