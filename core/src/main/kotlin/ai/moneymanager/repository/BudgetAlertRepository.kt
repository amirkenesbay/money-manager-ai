package ai.moneymanager.repository

import ai.moneymanager.repository.entity.BudgetAlertEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface BudgetAlertRepository : MongoRepository<BudgetAlertEntity, ObjectId>
