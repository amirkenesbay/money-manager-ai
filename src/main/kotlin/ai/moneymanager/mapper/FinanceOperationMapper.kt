package ai.moneymanager.mapper

import ai.moneymanager.domain.model.FinanceOperation
import ai.moneymanager.repository.entity.FinanceOperationEntity
import kz.rmr.chatmachinist_mongo.mapper.Mapper
import org.springframework.stereotype.Component

interface FinanceOperationMapper: Mapper<FinanceOperation, FinanceOperationEntity>
@Component
class FinanceOperationMapperImpl : FinanceOperationMapper {

    override fun fromModel(model: FinanceOperation): FinanceOperationEntity =
        FinanceOperationEntity(
            id = model.id,
            telegramUserId = model.telegramUserId,
            groupId = model.groupId,
            categoryId = model.categoryId,
            day = model.day,
            amount = model.amount,
            operationType = model.operationType,
            currency = model.currency,
            description = model.description
        )

    override fun fromDocument(document: FinanceOperationEntity): FinanceOperation =
        FinanceOperation(
            id = document.id,
            telegramUserId = document.telegramUserId,
            groupId = document.groupId,
            categoryId = document.categoryId,
            day = document.day,
            amount = document.amount,
            operationType = document.operationType,
            currency = document.currency,
            description = document.description
        )
}