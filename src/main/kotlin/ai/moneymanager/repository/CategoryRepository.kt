package ai.moneymanager.repository

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.entity.CategoryEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : MongoRepository<CategoryEntity, ObjectId> {
    fun findByGroupId(groupId: ObjectId): List<CategoryEntity>
    fun findByGroupIdOrderByAuditInfoCreatedAtAsc(groupId: ObjectId): List<CategoryEntity>
    fun findByGroupIdAndType(groupId: ObjectId, type: CategoryType): List<CategoryEntity>
    fun findByGroupIdAndTypeOrderByAuditInfoCreatedAtAsc(groupId: ObjectId, type: CategoryType): List<CategoryEntity>
    fun findByGroupIdAndName(groupId: ObjectId, name: String): CategoryEntity?
    fun findByGroupIdAndNameAndType(groupId: ObjectId, name: String, type: CategoryType): CategoryEntity?
    fun deleteByGroupId(groupId: ObjectId): Long
}