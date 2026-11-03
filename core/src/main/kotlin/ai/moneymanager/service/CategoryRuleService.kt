package ai.moneymanager.service

import ai.moneymanager.domain.event.CategoryCorrected
import ai.moneymanager.domain.event.CategoryDeleted
import ai.moneymanager.domain.model.CategoryRule
import ai.moneymanager.domain.text.merchantKey
import ai.moneymanager.repository.CategoryRuleRepository
import ai.moneymanager.repository.entity.CategoryRuleEntity
import org.bson.types.ObjectId
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class CategoryRuleService(
    private val categoryRuleRepository: CategoryRuleRepository,
) {
    fun rules(groupId: ObjectId): List<CategoryRule> =
        categoryRuleRepository.findByGroupIdOrderByAuditInfoModifiedAtDesc(groupId).map { it.toModel() }

    fun forget(groupId: ObjectId, ruleId: ObjectId): Boolean {
        val rule = categoryRuleRepository.findByGroupIdAndId(groupId, ruleId) ?: return false
        categoryRuleRepository.delete(rule)
        return true
    }

    @EventListener
    fun onCategoryCorrected(event: CategoryCorrected) {
        val key = merchantKey(event.description) ?: return
        val existing = categoryRuleRepository.findByGroupIdAndMerchantKey(event.groupId, key)
        categoryRuleRepository.save(
            existing?.copy(categoryId = event.categoryId)
                ?: CategoryRuleEntity(groupId = event.groupId, merchantKey = key, categoryId = event.categoryId)
        )
    }

    @EventListener
    fun onCategoryDeleted(event: CategoryDeleted) {
        categoryRuleRepository.deleteByCategoryId(event.categoryId)
    }

    private fun CategoryRuleEntity.toModel() = CategoryRule(checkNotNull(id), merchantKey, categoryId)
}
