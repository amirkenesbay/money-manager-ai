package ai.moneymanager

import ai.moneymanager.chat.config.GeminiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties::class)
class MoneyManagerAIApplication

fun main(args: Array<String>) {
    runApplication<MoneyManagerAIApplication>(*args)
}

/*
TODO: - Add alternative cases when user do something incorrect in transition state (for example, type incorrect date)
TODO: - Edit transaction operations —> income and expense
TODO: - Add condition when starting transition for fin operation: if balance is empty, then start the balance transitionS
TODO: - Add date for today and other date buttons. Example: button text “today 02.04”, “yesterday 01.04”
TODO: - Divide categories to income and expense Example:  categories for income —> salary, investment, courses categories for expense —> health, cafe, education
TODO: - Add income and expense history
TODO: - Add document export for financial operations
TODO: - Add statistics for income/expense
TODO: - Add an option to add emoji when adding new category.   Example: select category —> car —> type the emoji for the category —>  🚗 —> Category added: car 🚗  Or automatically add the emoji by name of the category
TODO: - Add photo transition for financial operations
TODO: - Add pagination for categories
TODO: - Add guards (checks) for added category and added date.he
TODO: - Translate the date of transactional operations to UTC+5
TODO: - Add the amount along the chain, then category, date, description
TODO: - When adding a financial transaction, for example, an expense and income, then give the opportunity to create a category. Same thing for the date
TODO: - Add keyboard buttons
TODO: - Think about val and var in CategoryEntity
TODO: - Add button to skip back to category choices when category deleted or updated
TODO: - Add check for unique categories when adding and updating
TODO: - Provide to add multiple categories separated by a comma or space, or by a new paragraph
TODO: - When deleting a category in the bot, display the message “Are you sure you want to delete?” and after that the user must enter the message "yes"
 */