package ai.moneymanager.domain.model.nlp

/**
 * Функции бота, которые Gemini может вызывать через Function Calling.
 * Эти методы используются для определения схемы вызова.
 */
object BotFunctions {

    /**
     * Создать новую группу для совместного учета финансов
     * @param groupName название группы (например: "Друзья", "Семья", "Работа")
     * @return JSON с результатом операции
     */
    @JvmStatic
    fun createGroup(groupName: String?) {
    }

    /**
     * Добавить расход (трату денег)
     * @param amount сумма расхода в тенге
     * @param category категория расхода (например: "Продукты", "Транспорт", "Развлечения")
     * @param description описание расхода (что именно купили)
     * @return JSON с результатом операции
     */
    @JvmStatic
    fun addExpense(amount: Double, category: String?, description: String?) {
    }

    /**
     * Добавить доход (получение денег)
     * @param amount сумма дохода в тенге
     * @param category категория дохода (например: "Зарплата", "Подарки", "Фриланс")
     * @param description описание дохода (откуда получены деньги)
     * @return JSON с результатом операции
     */
    @JvmStatic
    fun addIncome(amount: Double, category: String?, description: String?) {
    }

    /**
     * Удалить группу совместного учета финансов
     * @param groupName название группы для удаления (например: "Друзья", "Семья", "Работа")
     * @return JSON с результатом операции
     */
    @JvmStatic
    fun deleteGroup(groupName: String?) {
    }

    /**
     * Сообщение не относится к функциям бота финансового учета.
     * Вызывается когда пользователь спрашивает что-то не связанное с финансами или управлением группами.
     * @param originalMessage исходное сообщение пользователя
     * @return JSON с информацией что сообщение вне контекста
     */
    @JvmStatic
    fun outOfContext(originalMessage: String) {}

    // ===== CATEGORY =====

    /**
     * Создать новую категорию расхода или дохода в активной группе пользователя.
     * @param name название категории (например: "Кино", "Спорт", "Книги")
     * @param type тип категории: "EXPENSE" для расхода, "INCOME" для дохода
     * @param icon опциональная иконка-эмодзи для категории (например: "🎬", "⚽")
     */
    @JvmStatic
    fun createCategory(name: String, type: String, icon: String?) {
    }

    /**
     * Удалить категорию по названию.
     * @param name название категории для удаления
     * @param type опциональный тип категории: "EXPENSE" или "INCOME". Если не указан — поиск по всем типам.
     */
    @JvmStatic
    fun deleteCategory(name: String, type: String?) {
    }

    /**
     * Переименовать существующую категорию.
     * @param oldName текущее название категории
     * @param newName новое название
     * @param type опциональный тип категории: "EXPENSE" или "INCOME"
     */
    @JvmStatic
    fun renameCategory(oldName: String, newName: String, type: String?) {
    }

    /**
     * Изменить иконку-эмодзи у существующей категории.
     * @param name название категории
     * @param newIcon новая иконка-эмодзи (например: "🎬", "⚽", "💵")
     * @param type опциональный тип категории: "EXPENSE" или "INCOME"
     */
    @JvmStatic
    fun changeCategoryIcon(name: String, newIcon: String, type: String?) {
    }

    /**
     * Удалить ВСЕ категории в активной группе пользователя. Деструктивное действие.
     */
    @JvmStatic
    fun deleteAllCategories() {
    }

    /**
     * Показать список категорий пользователя.
     * @param type опциональный тип: "EXPENSE" — только расходные, "INCOME" — только доходные. Если не указан — все.
     */
    @JvmStatic
    fun listCategories(type: String?) {
    }
}