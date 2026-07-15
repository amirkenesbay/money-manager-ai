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
     * @param category expense category, in the user's original language. If a semantically matching category exists in the user's active categories list, pass its EXACT name. Otherwise propose a generalized name (e.g. "Продукты" instead of "Кола", "Транспорт" instead of "Бензин").
     * @param description short description of what was bought
     * @param suggestedCategoryIcon emoji to use ONLY when the proposed category does not yet exist in the user's active list and the bot will need to create it. One emoji that fits the generalized category (e.g. "🛒" for groceries, "🚗" for transport). Pass null when reusing an existing category.
     * @return JSON with the operation result
     */
    @JvmStatic
    fun addExpense(amount: Double, category: String?, description: String?, suggestedCategoryIcon: String?) {
    }

    /**
     * Add an income (money received).
     * @param amount income amount in tenge (KZT)
     * @param category income category, in the user's original language. If a semantically matching category exists in the user's active categories list, pass its EXACT name. Otherwise propose a generalized name.
     * @param description short description of where the money came from
     * @param suggestedCategoryIcon emoji to use ONLY when the proposed category does not yet exist in the user's active list. Pass null when reusing an existing category.
     * @return JSON with the operation result
     */
    @JvmStatic
    fun addIncome(amount: Double, category: String?, description: String?, suggestedCategoryIcon: String?) {
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

    // ===== GROUPS =====

    /**
     * Show the user's list of finance groups.
     */
    @JvmStatic
    fun listGroups() {
    }

    /**
     * Switch the user's current (active) group — new records will go into it.
     * @param groupName name of the group to switch to, in the user's original language
     */
    @JvmStatic
    fun switchGroup(groupName: String) {
    }

    // ===== BALANCE / REPORTS / HISTORY =====

    /**
     * Show the current group's balance (initial + income − expenses).
     */
    @JvmStatic
    fun showBalance() {
    }

    /**
     * Show the monthly analytics report (totals, top categories, max expense).
     * @param month optional month number 1-12; defaults to the current month
     * @param year optional year (e.g. 2026); defaults to the current year
     */
    @JvmStatic
    fun showReport(month: Double?, year: Double?) {
    }

    /**
     * Show the operations history for a period.
     * @param startDate optional period start in ISO format YYYY-MM-DD; defaults to the first day of the current month
     * @param endDate optional period end in ISO format YYYY-MM-DD; defaults to today
     */
    @JvmStatic
    fun showHistory(startDate: String?, endDate: String?) {
    }

    // ===== NOTIFICATIONS =====

    /**
     * Show the user's list of reminders/notifications.
     */
    @JvmStatic
    fun listNotifications() {
    }

    /**
     * Create a DAILY reminder at the given time.
     * @param name reminder name, in the user's original language (e.g. "Записать расходы")
     * @param hour hour 0-23 in the user's timezone
     * @param minute minute 0-59; defaults to 0 when the user did not specify minutes
     */
    @JvmStatic
    fun createNotification(name: String, hour: Double, minute: Double?) {
    }

    /**
     * Delete a reminder by name.
     * @param name reminder name to delete, in the user's original language
     */
    @JvmStatic
    fun deleteNotification(name: String) {
    }
}
