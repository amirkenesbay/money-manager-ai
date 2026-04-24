package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.entity.CategoryEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
    private val localizationService: LocalizationService
) {

    private val log = LoggerFactory.getLogger(CategoryService::class.java)

    fun createCategory(name: String, icon: String?, type: CategoryType, groupId: ObjectId): Category? {
        val existingCategory = categoryRepository.findByGroupIdAndNameAndType(groupId, name, type)
        if (existingCategory != null) {
            return null
        }

        val categoryEntity = CategoryEntity(
            name = name,
            icon = icon,
            type = type,
            groupId = groupId
        )

        val savedCategory = categoryRepository.save(categoryEntity)
        return mapToModel(savedCategory)
    }

    fun getCategoriesByGroup(groupId: ObjectId): List<Category> {
        return categoryRepository.findByGroupIdOrderByAuditInfoCreatedAtAsc(groupId)
            .map { mapToModel(it) }
    }

    fun getCategoriesByGroupAndType(groupId: ObjectId, type: CategoryType): List<Category> {
        return categoryRepository.findByGroupIdAndTypeOrderByAuditInfoCreatedAtAsc(groupId, type)
            .map { mapToModel(it) }
    }

    fun getCategory(categoryId: ObjectId): Category? {
        return categoryRepository.findById(categoryId)
            .map { mapToModel(it) }
            .orElse(null)
    }

    fun updateCategoryName(categoryId: ObjectId, newName: String): Category? {
        val categoryEntity = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val updatedCategory = categoryEntity.copy(name = newName)
        val savedCategory = categoryRepository.save(updatedCategory)
        return mapToModel(savedCategory)
    }

    fun updateCategoryIcon(categoryId: ObjectId, newIcon: String?): Category? {
        val categoryEntity = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val updatedCategory = categoryEntity.copy(icon = newIcon)
        val savedCategory = categoryRepository.save(updatedCategory)
        return mapToModel(savedCategory)
    }

    fun deleteCategory(categoryId: ObjectId): Boolean {
        return try {
            categoryRepository.deleteById(categoryId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * @param groupId ID группы
     * @return количество удаленных категорий
     */
    fun deleteAllCategoriesForGroup(groupId: ObjectId): Long {
        return try {
            categoryRepository.deleteByGroupId(groupId)
        } catch (e: Exception) {
            log.error("Error deleting all categories for group $groupId: ${e.message}", e)
            0L
        }
    }

    /**
     * @param sourceGroupId ID группы-источника
     * @param targetGroupId ID группы-приемника
     * @return количество скопированных категорий
     */
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
            groupId = entity.groupId
        )
    }
}
