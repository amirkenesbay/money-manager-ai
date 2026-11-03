package ai.moneymanager.repository

import ai.moneymanager.repository.entity.CategoryRuleEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface CategoryRuleRepository : MongoRepository<CategoryRuleEntity, ObjectId> {
    fun findByGroupIdOrderByAuditInfoModifiedAtDesc(groupId: ObjectId): List<CategoryRuleEntity>
    fun findByGroupIdAndMerchantKey(groupId: ObjectId, merchantKey: String): CategoryRuleEntity?
    fun findByGroupIdAndId(groupId: ObjectId, id: ObjectId): CategoryRuleEntity?
    fun deleteByCategoryId(categoryId: ObjectId): Long
}
