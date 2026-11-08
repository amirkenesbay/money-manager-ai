package ai.moneymanager.service.nlp

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.RecurrenceUnit
import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.model.question.FinanceIntent
import ai.moneymanager.domain.model.question.PeriodPreset
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.Schema
import java.lang.reflect.Method

data class GeminiToolDocs(
    val parameters: Map<String, String> = emptyMap(),
    val functions: Map<String, GeminiFunctionDoc> = emptyMap(),
)

data class GeminiFunctionDoc(
    val description: String,
    val parameters: Map<String, String> = emptyMap(),
)

private val PARAMETER_VALUES: Map<String, List<String>> = mapOf(
    "intent" to FinanceIntent.entries.map { it.name },
    "period" to PeriodPreset.entries.map { it.name },
    "comparePeriod" to PeriodPreset.entries.map { it.name },
    "operationType" to CategoryType.entries.map { it.name },
    "type" to CategoryType.entries.map { it.name },
    "repeatUnit" to RecurrenceUnit.entries.map { it.name },
    "direction" to LoanDirection.entries.map { it.name },
)

fun GeminiToolDocs.declarationOf(method: Method): FunctionDeclaration {
    val functionDoc = functions[method.name]
    val declaration = functionDoc?.let { FunctionDeclaration.fromMethod(it.description, method) } ?: FunctionDeclaration.fromMethod(method)
    val parameters = declaration.parameters().orElse(null) ?: return declaration
    val properties = parameters.properties().orElse(emptyMap())
        .mapValues { (name, schema) -> schema.documented(functionDoc?.parameters?.get(name) ?: this.parameters[name], PARAMETER_VALUES[name]) }
    return declaration.toBuilder().parameters(parameters.toBuilder().properties(properties).build()).build()
}

private fun Schema.documented(description: String?, values: List<String>?): Schema {
    if (description == null && values == null) return this
    val builder = toBuilder()
    description?.let { builder.description(it) }
    values?.let { builder.enum_(it) }
    return builder.build()
}
