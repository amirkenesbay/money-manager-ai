package ai.moneymanager.repository

import ai.moneymanager.repository.entity.SettlementEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface SettlementRepository : MongoRepository<SettlementEntity, ObjectId> {
    fun findByGroupIdOrderBySettledOnDesc(groupId: ObjectId): List<SettlementEntity>
    fun findByGroupIdAndId(groupId: ObjectId, id: ObjectId): SettlementEntity?
}
