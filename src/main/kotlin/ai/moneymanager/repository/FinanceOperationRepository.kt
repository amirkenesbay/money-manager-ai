package ai.moneymanager.repository

import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FinanceOperationRepository: MongoRepository<FinanceOperationEntity,ObjectId> {}