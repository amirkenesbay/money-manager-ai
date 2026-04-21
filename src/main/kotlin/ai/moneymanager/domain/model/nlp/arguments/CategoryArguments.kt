package ai.moneymanager.domain.model.nlp.arguments

data class CreateCategoryArgs(
    val name: String,
    val type: String,
    val icon: String? = null
)

data class DeleteCategoryArgs(
    val name: String,
    val type: String? = null
)

data class RenameCategoryArgs(
    val oldName: String,
    val newName: String,
    val type: String? = null
)

data class ChangeCategoryIconArgs(
    val name: String,
    val newIcon: String,
    val type: String? = null
)

data class ListCategoriesArgs(
    val type: String? = null
)
