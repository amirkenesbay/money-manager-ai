package ai.moneymanager.service

import ai.moneymanager.domain.event.CategoryDeleted
import ai.moneymanager.domain.event.CategoryCreated
import ai.moneymanager.domain.event.CategoryUpdated
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryCreationResult
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.CategoryUpdateResult
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.entity.CategoryEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import ai.moneymanager.domain.model.BudgetLimitResult
import java.math.BigDecimal

private data class DefaultCategorySeed(val nameKey: String, val icon: String, val type: CategoryType)

private val DEFAULT_CATEGORY_SEEDS = listOf(
    DefaultCategorySeed("category.default.expense.groceries.name", "🛒", CategoryType.EXPENSE),
    DefaultCategorySeed("category.default.expense.transport.name", "🚗", CategoryType.EXPENSE),
    DefaultCategorySeed("category.default.expense.entertainment.name", "🎬", CategoryType.EXPENSE),
    DefaultCategorySeed("category.default.expense.health.name", "💊", CategoryType.EXPENSE),
    DefaultCategorySeed("category.default.expense.clothes.name", "👕", CategoryType.EXPENSE),
    DefaultCategorySeed("category.default.expense.education.name", "📚", CategoryType.EXPENSE),
    DefaultCategorySeed("category.default.expense.other.name", "📦", CategoryType.EXPENSE),
    DefaultCategorySeed("category.default.income.salary.name", "💰", CategoryType.INCOME),
    DefaultCategorySeed("category.default.income.gifts.name", "🎁", CategoryType.INCOME),
    DefaultCategorySeed("category.default.income.other.name", "📦", CategoryType.INCOME),
)

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val localizationService: LocalizationService,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(CategoryService::class.java)

    fun createCategory(actorId: Long, name: String, icon: String?, type: CategoryType, groupId: ObjectId, maxCategoriesPerType: Int): CategoryCreationResult {
        val existingCategory = categoryRepository.findByGroupIdAndNameAndType(groupId, name, type)
        if (existingCategory != null) {
            return CategoryCreationResult.Duplicate
        }

        val currentCount = categoryRepository.countByGroupIdAndType(groupId, type)
        if (currentCount >= maxCategoriesPerType.toLong()) {
            return CategoryCreationResult.LimitReached(maxCategoriesPerType)
        }

        val categoryEntity = CategoryEntity(
            name = name,
            icon = icon,
            type = type,
            groupId = groupId
        )

        val savedCategory = categoryRepository.save(categoryEntity)
        eventPublisher.publishEvent(CategoryCreated(groupId, actorId, savedCategory.icon, savedCategory.name))
        return CategoryCreationResult.Created(mapToModel(savedCategory))
    }

    fun getCategoriesByGroup(groupId: ObjectId): List<Category> {
        return ordered(categoryRepository.findByGroupIdOrderByAuditInfoCreatedAtAsc(groupId))
            .map { mapToModel(it) }
    }

    fun getCategoriesByGroupAndType(groupId: ObjectId, type: CategoryType): List<Category> {
        return ordered(categoryRepository.findByGroupIdAndTypeOrderByAuditInfoCreatedAtAsc(groupId, type))
            .map { mapToModel(it) }
    }

    fun reorder(groupId: ObjectId, categoryIds: List<ObjectId>): Boolean {
        val categories = categoryRepository.findByGroupIdOrderByAuditInfoCreatedAtAsc(groupId).associateBy { it.id }
        if (categoryIds.distinct().size != categoryIds.size || !categories.keys.containsAll(categoryIds)) return false
        categoryRepository.saveAll(categoryIds.mapIndexed { position, id -> checkNotNull(categories[id]).copy(position = position) })
        return true
    }

    private fun ordered(categories: List<CategoryEntity>): List<CategoryEntity> =
        categories.sortedBy { it.position ?: Int.MAX_VALUE }

    fun getCategory(categoryId: ObjectId): Category? {
        return categoryRepository.findById(categoryId)
            .map { mapToModel(it) }
            .orElse(null)
    }

    fun updateCategory(actorId: Long, categoryId: ObjectId, name: String, icon: String?): CategoryUpdateResult {
        val entity = categoryRepository.findById(categoryId).orElse(null) ?: return CategoryUpdateResult.NotFound
        val namesake = categoryRepository.findByGroupIdAndNameAndType(entity.groupId, name, entity.type)
        if (namesake != null && namesake.id != categoryId) return CategoryUpdateResult.Duplicate

        val saved = categoryRepository.save(entity.copy(name = name, icon = icon))
        if (entity.name != saved.name || entity.icon != saved.icon) {
            eventPublisher.publishEvent(CategoryUpdated(entity.groupId, actorId, entity.icon, entity.name, saved.icon, saved.name))
        }
        return CategoryUpdateResult.Updated(mapToModel(saved))
    }

    fun updateCategoryName(actorId: Long, categoryId: ObjectId, newName: String): Category? {
        val category = getCategory(categoryId) ?: return null
        return (updateCategory(actorId, categoryId, newName, category.icon) as? CategoryUpdateResult.Updated)?.category
    }

    fun updateCategoryIcon(actorId: Long, categoryId: ObjectId, newIcon: String?): Category? {
        val category = getCategory(categoryId) ?: return null
        return (updateCategory(actorId, categoryId, category.name, newIcon) as? CategoryUpdateResult.Updated)?.category
    }

    fun setMonthlyLimit(categoryId: ObjectId, limit: BigDecimal?, rollover: Boolean, maxBudgets: Int?): BudgetLimitResult {
        val entity = categoryRepository.findById(categoryId).orElse(null) ?: return BudgetLimitResult.NotFound
        if (entity.type != CategoryType.EXPENSE) return BudgetLimitResult.NotExpense
        val othersWithLimit = categoryRepository.findByGroupIdAndMonthlyLimitIsNotNull(entity.groupId).count { it.id != categoryId }
        if (limit != null && maxBudgets != null && othersWithLimit >= maxBudgets) return BudgetLimitResult.LimitReached(maxBudgets)
        return BudgetLimitResult.Updated(mapToModel(categoryRepository.save(entity.copy(monthlyLimit = limit, rollover = rollover && limit != null))))
    }

    fun getBudgetCategories(groupId: ObjectId): List<Category> =
        categoryRepository.findByGroupIdAndMonthlyLimitIsNotNull(groupId).map { mapToModel(it) }

    fun deleteCategory(actorId: Long, categoryId: ObjectId): Boolean {
        val entity = categoryRepository.findById(categoryId).orElse(null) ?: return false
        categoryRepository.delete(entity)
        eventPublisher.publishEvent(CategoryDeleted(entity.groupId, categoryId, actorId, entity.icon, entity.name))
        return true
    }

    fun deleteAllCategoriesForGroup(groupId: ObjectId): Long {
        return try {
            categoryRepository.deleteByGroupId(groupId)
        } catch (e: Exception) {
            log.error("Error deleting all categories for group $groupId: ${e.message}", e)
            0L
        }
    }

    fun copyCategoriesFromGroup(sourceGroupId: ObjectId, targetGroupId: ObjectId): Int {
        val sourceCategories = categoryRepository.findByGroupId(sourceGroupId)

        if (sourceCategories.isEmpty()) {
            return 0
        }

        val copiedCategories = sourceCategories.map { source ->
            CategoryEntity(
                id = null,
                name = source.name,
                icon = source.icon,
                type = source.type,
                groupId = targetGroupId
            )
        }

        val saved = categoryRepository.saveAll(copiedCategories)
        log.info("Copied ${saved.count()} categories from group $sourceGroupId to $targetGroupId")
        return saved.count()
    }

    fun createDefaultCategories(groupId: ObjectId, language: String) {
        val entities = DEFAULT_CATEGORY_SEEDS.map { seed ->
            CategoryEntity(
                name = localizationService.t(seed.nameKey, language),
                icon = seed.icon,
                type = seed.type,
                groupId = groupId
            )
        }
        categoryRepository.saveAll(entities)
    }

    private fun mapToModel(entity: CategoryEntity): Category {
        return Category(
            id = entity.id,
            name = entity.name,
            icon = entity.icon,
            type = entity.type,
            groupId = entity.groupId,
            monthlyLimit = entity.monthlyLimit,
            rollover = entity.rollover,
        )
    }
}
