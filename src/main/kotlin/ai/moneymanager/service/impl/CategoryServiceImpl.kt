package ai.moneymanager.service.impl

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.entity.CategoryEntity
import ai.moneymanager.service.CategoryService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository
) : CategoryService {

    /**
     * Создать новую категорию
     */
    override fun createCategory(name: String, icon: String?, type: CategoryType, groupId: ObjectId): Category? {
        // Проверяем, не существует ли уже категория с таким названием в этой группе
        val existingCategory = categoryRepository.findByGroupIdAndName(groupId, name)
        if (existingCategory != null) {
            // Категория с таким названием уже существует
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

    /**
     * Получить все категории группы
     */
    override fun getCategoriesByGroup(groupId: ObjectId): List<Category> {
        return categoryRepository.findByGroupId(groupId)
            .map { mapToModel(it) }
    }

    /**
     * Получить категории группы по типу
     */
    override fun getCategoriesByGroupAndType(groupId: ObjectId, type: CategoryType): List<Category> {
        return categoryRepository.findByGroupIdAndType(groupId, type)
            .map { mapToModel(it) }
    }

    /**
     * Получить категорию по ID
     */
    override fun getCategory(categoryId: ObjectId): Category? {
        return categoryRepository.findById(categoryId)
            .map { mapToModel(it) }
            .orElse(null)
    }

    /**
     * Обновить название категории
     */
    override fun updateCategoryName(categoryId: ObjectId, newName: String): Category? {
        val categoryEntity = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val updatedCategory = categoryEntity.copy(name = newName)
        val savedCategory = categoryRepository.save(updatedCategory)
        return mapToModel(savedCategory)
    }

    /**
     * Обновить иконку категории
     */
    override fun updateCategoryIcon(categoryId: ObjectId, newIcon: String?): Category? {
        val categoryEntity = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val updatedCategory = categoryEntity.copy(icon = newIcon)
        val savedCategory = categoryRepository.save(updatedCategory)
        return mapToModel(savedCategory)
    }

    /**
     * Удалить категорию
     */
    override fun deleteCategory(categoryId: ObjectId): Boolean {
        return try {
            categoryRepository.deleteById(categoryId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Удалить все категории группы
     *
     * @param groupId ID группы
     * @return количество удаленных категорий
     */
    override fun deleteAllCategoriesForGroup(groupId: ObjectId): Long {
        return try {
            categoryRepository.deleteByGroupId(groupId)
        } catch (e: Exception) {
            println("❌ Error deleting all categories for group $groupId: ${e.message}")
            0L
        }
    }

    /**
     * Копирует все категории из одной группы в другую
     *
     * @param sourceGroupId ID группы-источника
     * @param targetGroupId ID группы-приемника
     * @return количество скопированных категорий
     */
    override fun copyCategoriesFromGroup(sourceGroupId: ObjectId, targetGroupId: ObjectId): Int {
        // Загрузить все категории из источника
        val sourceCategories = categoryRepository.findByGroupId(sourceGroupId)

        // Если категорий нет - вернуть 0
        if (sourceCategories.isEmpty()) {
            return 0
        }

        // Создать новые категории с новым groupId
        val copiedCategories = sourceCategories.map { source ->
            CategoryEntity(
                id = null,  // Новый ObjectId будет сгенерирован MongoDB
                name = source.name,
                icon = source.icon,
                type = source.type,
                groupId = targetGroupId  // КЛЮЧЕВОЕ изменение
            )
        }

        // Сохранить все скопированные категории
        val saved = categoryRepository.saveAll(copiedCategories)

        println("✅ Copied ${saved.count()} categories from group $sourceGroupId to $targetGroupId")

        return saved.count()
    }

    /**
     * Создать дефолтные категории для новой группы
     */
    override fun createDefaultCategories(groupId: ObjectId) {
        val defaultExpenseCategories = listOf(
            CategoryEntity(name = "Продукты", icon = "🛒", type = CategoryType.EXPENSE, groupId = groupId),
            CategoryEntity(name = "Транспорт", icon = "🚗", type = CategoryType.EXPENSE, groupId = groupId),
            CategoryEntity(name = "Развлечения", icon = "🎬", type = CategoryType.EXPENSE, groupId = groupId),
            CategoryEntity(name = "Здоровье", icon = "💊", type = CategoryType.EXPENSE, groupId = groupId),
            CategoryEntity(name = "Одежда", icon = "👕", type = CategoryType.EXPENSE, groupId = groupId),
            CategoryEntity(name = "Образование", icon = "📚", type = CategoryType.EXPENSE, groupId = groupId),
            CategoryEntity(name = "Другое", icon = "📦", type = CategoryType.EXPENSE, groupId = groupId)
        )

        val defaultIncomeCategories = listOf(
            CategoryEntity(name = "Зарплата", icon = "💰", type = CategoryType.INCOME, groupId = groupId),
            CategoryEntity(name = "Подарки", icon = "🎁", type = CategoryType.INCOME, groupId = groupId),
            CategoryEntity(name = "Другое", icon = "📦", type = CategoryType.INCOME, groupId = groupId)
        )

        categoryRepository.saveAll(defaultExpenseCategories + defaultIncomeCategories)
    }

    /**
     * Маппинг Entity -> Model
     */
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