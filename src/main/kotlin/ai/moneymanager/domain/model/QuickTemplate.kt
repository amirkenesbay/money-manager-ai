package ai.moneymanager.domain.model

data class QuickNotificationTemplate(
    val buttonType: MoneyManagerButtonType,
    val name: String,
    val icon: String
)

data class QuickGroupTemplate(
    val buttonType: MoneyManagerButtonType,
    val nameKey: String
)

data class QuickCategoryTemplate(
    val buttonType: MoneyManagerButtonType,
    val nameKey: String,
    val icon: String,
    val type: CategoryType
)

object QuickTemplates {
    val GROUPS = listOf(
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_FAMILY, "group.quick.family.name"),
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_WITH_FRIEND, "group.quick.with_friend.name"),
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_WORK, "group.quick.work.name"),
        QuickGroupTemplate(MoneyManagerButtonType.QUICK_GROUP_TRIP, "group.quick.trip.name"),
    )

    val EXPENSE_CATEGORIES = listOf(
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_FOOD_OUT, "category.quick.food_out.name", "🍔", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_UTILITIES, "category.quick.utilities.name", "🏠", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_MEDICINE, "category.quick.medicine.name", "💊", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_ENTERTAINMENT, "category.quick.entertainment.name", "🎮", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_CLOTHES, "category.quick.clothes.name", "👕", CategoryType.EXPENSE),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_TAXI, "category.quick.taxi.name", "🚕", CategoryType.EXPENSE),
    )

    val INCOME_CATEGORIES = listOf(
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_SALARY, "category.quick.salary.name", "💰", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_BONUS, "category.quick.bonus.name", "💸", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_GIFT, "category.quick.gift.name", "🎁", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_FREELANCE, "category.quick.freelance.name", "💼", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_INVESTMENTS, "category.quick.investments.name", "📈", CategoryType.INCOME),
        QuickCategoryTemplate(MoneyManagerButtonType.QUICK_CATEGORY_DEBT_RETURN, "category.quick.debt_return.name", "💵", CategoryType.INCOME),
    )

    val ALL_CATEGORIES = EXPENSE_CATEGORIES + INCOME_CATEGORIES

    val NOTIFICATIONS = listOf(
        QuickNotificationTemplate(MoneyManagerButtonType.QUICK_NOTIF_DAILY_EXPENSES, "Внести расходы за день", "📝"),
        QuickNotificationTemplate(MoneyManagerButtonType.QUICK_NOTIF_WEEKLY_REPORT, "Еженедельный учёт", "📊"),
        QuickNotificationTemplate(MoneyManagerButtonType.QUICK_NOTIF_MONTHLY_BUDGET, "Проверить бюджет", "💰"),
        QuickNotificationTemplate(MoneyManagerButtonType.QUICK_NOTIF_SALARY_DAY, "День зарплаты", "💸"),
    )
}
