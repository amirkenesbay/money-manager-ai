package ai.moneymanager.domain.model.nlp

/**
 * Bot functions that Gemini can invoke via Function Calling.
 * These method signatures are used to derive the call schema.
 */
object BotFunctions {

    /**
     * Create a new shared finance group.
     * @param groupName name of the group, in the user's original language and casing (e.g. "Friends", "Семья", "Достар")
     * @return JSON with the operation result
     */
    @JvmStatic
    fun createGroup(groupName: String?) {
    }

    /**
     * Add an expense (money spent).
     * @param amount expense amount in tenge (KZT)
     * @param category expense category, in the user's original language (e.g. "Groceries", "Продукты", "Азық-түлік")
     * @param description short description of what was bought
     * @return JSON with the operation result
     */
    @JvmStatic
    fun addExpense(amount: Double, category: String?, description: String?) {
    }

    /**
     * Add an income (money received).
     * @param amount income amount in tenge (KZT)
     * @param category income category, in the user's original language (e.g. "Salary", "Зарплата", "Жалақы")
     * @param description short description of where the money came from
     * @return JSON with the operation result
     */
    @JvmStatic
    fun addIncome(amount: Double, category: String?, description: String?) {
    }

    /**
     * Delete a shared finance group.
     * @param groupName name of the group to delete, in the user's original language
     * @return JSON with the operation result
     */
    @JvmStatic
    fun deleteGroup(groupName: String?) {
    }

    /**
     * The message is not related to the finance bot's functions.
     * Called when the user asks something unrelated to finance, categories, or groups.
     * @param originalMessage the user's original message
     * @return JSON with out-of-context info
     */
    @JvmStatic
    fun outOfContext(originalMessage: String) {}

    // ===== CATEGORY =====

    /**
     * Create a new expense or income category in the user's active group.
     * @param name category name, in the user's original language (e.g. "Movies", "Кино", "Кино")
     * @param type category type: "EXPENSE" or "INCOME" (uppercase Latin)
     * @param icon optional emoji icon for the category (e.g. "🎬", "⚽")
     */
    @JvmStatic
    fun createCategory(name: String, type: String, icon: String?) {
    }

    /**
     * Delete a category by name.
     * @param name category name to delete, in the user's original language
     * @param type optional category type: "EXPENSE" or "INCOME". If omitted — search across all types.
     */
    @JvmStatic
    fun deleteCategory(name: String, type: String?) {
    }

    /**
     * Rename an existing category.
     * @param oldName current category name, in the user's original language
     * @param newName new category name, in the user's original language
     * @param type optional category type: "EXPENSE" or "INCOME"
     */
    @JvmStatic
    fun renameCategory(oldName: String, newName: String, type: String?) {
    }

    /**
     * Change the emoji icon of an existing category.
     * @param name category name, in the user's original language
     * @param newIcon new emoji icon (e.g. "🎬", "⚽", "💵")
     * @param type optional category type: "EXPENSE" or "INCOME"
     */
    @JvmStatic
    fun changeCategoryIcon(name: String, newIcon: String, type: String?) {
    }

    /**
     * Delete ALL categories in the user's active group. Destructive action.
     */
    @JvmStatic
    fun deleteAllCategories() {
    }

    /**
     * Show the user's list of categories.
     * @param type optional filter: "EXPENSE" — only expense categories, "INCOME" — only income. If omitted — all.
     */
    @JvmStatic
    fun listCategories(type: String?) {
    }
}
