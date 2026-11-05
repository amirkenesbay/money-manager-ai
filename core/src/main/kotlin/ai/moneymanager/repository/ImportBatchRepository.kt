package ai.moneymanager.repository

import ai.moneymanager.repository.entity.ImportBatchEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface ImportBatchRepository : MongoRepository<ImportBatchEntity, ObjectId> {
    fun countByUserIdAndUndoneFalse(userId: Long): Long
    fun findByIdAndUserId(id: ObjectId, userId: Long): ImportBatchEntity?
}
