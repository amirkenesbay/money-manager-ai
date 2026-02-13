package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import org.bson.types.ObjectId

interface CategoryService {
    /**
     * Создать новую категорию
     */
    fun createCategory(name: String, icon: String?, type: CategoryType, groupId: ObjectId): Category?
    /**
     * Получить все категории группы
     */
    fun getCategoriesByGroup(groupId: ObjectId): List<Category>

    /**
     * Получить категории группы по типу
     */
    fun getCategoriesByGroupAndType(groupId: ObjectId, type: CategoryType): List<Category>

    /**
     * Получить категорию по ID
     */
    fun getCategory(categoryId: ObjectId): Category?

    /**
     * Обновить название категории
     */
    fun updateCategoryName(categoryId: ObjectId, newName: String): Category?

    /**
     * Обновить иконку категории
     */
    fun updateCategoryIcon(categoryId: ObjectId, newIcon: String?): Category?

    /**
     * Удалить категорию
     */
    fun deleteCategory(categoryId: ObjectId): Boolean

    /**
     * Удалить все категории группы
     *
     * @param groupId ID группы
     * @return количество удаленных категорий
     */
    fun deleteAllCategoriesForGroup(groupId: ObjectId): Int

    /**
     * Копирует все категории из одной группы в другую
     *
     * @param sourceGroupId ID группы-источника
     * @param targetGroupId ID группы-приемника
     * @return количество скопированных категорий
     */
    fun copyCategoriesFromGroup(sourceGroupId: ObjectId, targetGroupId: ObjectId): Int

    /**
     * Создать дефолтные категории для новой группы
     */
    fun createDefaultCategories(groupId: ObjectId)
}