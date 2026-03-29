package ai.moneymanager.domain.model

data class QuickGroupTemplate(
    val buttonType: MoneyManagerButtonType,
    val name: String
)

data class QuickCategoryTemplate(
    val buttonType: MoneyManagerButtonType,
    val name: String,
    val icon: String,
    val type: CategoryType
)

object QuickTemplates {
    val GROUPS = listOf(
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_FAMILY, "Семья"),
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_WITH_FRIEND, "С другом"),
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_WORK, "Работа"),
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_TRIP, "Поездка"),
    )

    val EXPENSE_CATEGORIES = listOf(
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_FOOD_OUT, "Еда вне дома", "🍔", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_UTILITIES, "ЖКХ", "🏠", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_MEDICINE, "Медицина", "💊", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_ENTERTAINMENT, "Развлечения", "🎮", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_CLOTHES, "Одежда и обувь", "👕", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_TAXI, "Такси", "🚕", CategoryType.EXPENSE),
    )

    val INCOME_CATEGORIES = listOf(
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_SALARY, "Зарплата", "💰", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_BONUS, "Премия", "💸", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_GIFT, "Подарок", "🎁", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_FREELANCE, "Фриланс", "💼", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_INVESTMENTS, "Инвестиции", "📈", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_DEBT_RETURN, "Возврат долга", "💵", CategoryType.INCOME),
    )

    val ALL_CATEGORIES = EXPENSE_CATEGORIES + INCOME_CATEGORIES
}
