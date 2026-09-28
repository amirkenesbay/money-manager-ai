package ai.moneymanager.repository

import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface FinanceOperationRepositoryCustom {
    fun findPage(filter: OperationFilter, pageable: Pageable): Page<FinanceOperationEntity>
}
