package ai.moneymanager.repository

import ai.moneymanager.dto.HistoryFinanceOperationDto
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface FinanceOperationRepository : MongoRepository<FinanceOperationEntity, ObjectId> {

    @Aggregation(
        pipeline = [
            "{ '\$match': { 'groupId': ?0 } }",
            "{ '\$group': { '_id': null, 'balance': { '\$sum': '\$amount' } } }"
        ]
    )
    fun getBalanceByGroupId(groupId: ObjectId): BigDecimal?

    @Aggregation(
        pipeline = [
            "{ '\$match': { 'groupId': ?0 } }",
            "{ '\$sort': { 'auditInfo.created_at': 1 } }",
            "{ '\$project': { " +
                    "'_id': 0, " +
                    "'telegramUserId': 1, " +
                    "'amount': 1, " +
                    "'currency': 1, " +
                    //"'day': 1, " +
                    "'description': 1, " +
                    "'createdAt': '\$auditInfo.created_at' } }"
        ]
    )
    fun getAllHistoryFinanceOperation(groupId: ObjectId): List<HistoryFinanceOperationDto>

    @Aggregation(
        pipeline = [
        "{ '\$match': { " +
                "'groupId': ?0,'auditInfo.created_at': { '\$gte': ?1, '\$lte': ?2 } } }",
        "{ '\$sort': { 'auditInfo.created_at': 1 } }",
        "{ '\$project': { " +
                "'_id': 0, " +
                "'telegramUserId': 1, " +
                "'amount': 1, " +
                "'currency': 1, " +
                //"'day': 1, " +
                "'description': 1, " +
                "'createdAt': '\$auditInfo.created_at' } }"
    ]
    )
    fun getHistoryFinanceOperationByPeriod(groupId: ObjectId, from: LocalDateTime, to: LocalDateTime): List<HistoryFinanceOperationDto>
}