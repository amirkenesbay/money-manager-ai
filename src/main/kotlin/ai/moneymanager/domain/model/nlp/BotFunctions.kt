package ai.moneymanager.domain.model.nlp

object BotFunctions {
    @JvmStatic
    fun createGroup(groupName: String?) {
    }

    @JvmStatic
    fun addExpense(amount: Double, category: String?, description: String?, suggestedCategoryIcon: String?, operationDate: String?) {
    }

    @JvmStatic
    fun addIncome(amount: Double, category: String?, description: String?, suggestedCategoryIcon: String?, operationDate: String?) {
    }

    @JvmStatic
    fun deleteGroup(groupName: String?) {
    }

    @JvmStatic
    fun outOfContext(originalMessage: String) {}

    @JvmStatic
    fun createCategory(name: String, type: String, icon: String?) {
    }

    @JvmStatic
    fun deleteCategory(name: String, type: String?) {
    }

    @JvmStatic
    fun renameCategory(oldName: String, newName: String, type: String?) {
    }

    @JvmStatic
    fun changeCategoryIcon(name: String, newIcon: String, type: String?) {
    }

    @JvmStatic
    fun deleteAllCategories() {
    }

    @JvmStatic
    fun listCategories(type: String?) {
    }

    @JvmStatic
    fun listGroups() {
    }

    @JvmStatic
    fun switchGroup(groupName: String) {
    }

    @JvmStatic
    fun showBalance() {
    }

    @JvmStatic
    fun showReport(month: Double?, year: Double?) {
    }

    @JvmStatic
    fun showHistory(startDate: String?, endDate: String?, type: String?, categoryFilter: String?) {
    }

    @JvmStatic
    fun listNotifications() {
    }

    @JvmStatic
    fun createNotification(name: String, hour: Double, minute: Double?) {
    }

    @JvmStatic
    fun deleteNotification(name: String) {
    }

    @JvmStatic
    fun deleteLastOperation(type: String?) {
    }

    @JvmStatic
    fun editLastOperation(type: String?, newAmount: Double?, newCategory: String?, newOperationDate: String?) {
    }
}
