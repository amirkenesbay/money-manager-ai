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
     * Сообщение не относится к функциям бота финансового учета.
     * Вызывается когда пользователь спрашивает что-то не связанное с финансами или управлением группами.
     * @param originalMessage исходное сообщение пользователя
     * @return JSON с информацией что сообщение вне контекста
     */
    @JvmStatic
    fun outOfContext(originalMessage: String) {}
}