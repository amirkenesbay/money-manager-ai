package ai.moneymanager.domain.model.nlp

object BotFunctions {
    @JvmStatic
    fun createGroup(groupName: String?) {
    }

    @JvmStatic
    fun addExpense(amount: Double, category: String?, description: String?, suggestedCategoryIcon: String?, operationDate: String?, repeatUnit: String?, repeatInterval: Double?, repeatPhrase: String?, paidBy: String?, splitWith: String?, currency: String?) {
    }

    @JvmStatic
    fun addIncome(amount: Double, category: String?, description: String?, suggestedCategoryIcon: String?, operationDate: String?, repeatUnit: String?, repeatInterval: Double?, repeatPhrase: String?, currency: String?) {
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

    @JvmStatic
    fun askFinance(
        intent: String,
        operationType: String?,
        categories: String?,
        period: String?,
        month: String?,
        from: String?,
        to: String?,
        comparePeriod: String?,
        compareMonth: String?,
        member: String?,
        place: String?,
        tag: String?,
        minAmount: Double?,
        limit: Double?,
        amount: Double?,
    ) {
    }

    @JvmStatic
    fun addLoan(direction: String, person: String, amount: Double, dueDate: String?, note: String?) {
    }

    @JvmStatic
    fun repayLoan(direction: String, person: String, amount: Double?) {
    }
}
