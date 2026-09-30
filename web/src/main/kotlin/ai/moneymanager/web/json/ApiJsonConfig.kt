package ai.moneymanager.web.json

import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule
import java.math.BigDecimal

private const val INVALID_OBJECT_ID_MESSAGE = "not a valid ObjectId"

@Configuration
class ApiJsonConfig {
    @Bean
    fun apiJsonModule(): JacksonModule = SimpleModule()
        .addSerializer(BigDecimal::class.java, PlainBigDecimalSerializer)
        .addSerializer(ObjectId::class.java, ObjectIdSerializer)
        .addDeserializer(ObjectId::class.java, ObjectIdDeserializer)
}

private object PlainBigDecimalSerializer : ValueSerializer<BigDecimal>() {
    override fun serialize(value: BigDecimal, generator: JsonGenerator, context: SerializationContext) {
        generator.writeString(value.toPlainString())
    }
}

private object ObjectIdSerializer : ValueSerializer<ObjectId>() {
    override fun serialize(value: ObjectId, generator: JsonGenerator, context: SerializationContext) {
        generator.writeString(value.toHexString())
    }
}

private object ObjectIdDeserializer : ValueDeserializer<ObjectId>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ObjectId {
        val text = parser.valueAsString
        if (text == null || !ObjectId.isValid(text)) {
            return context.handleWeirdStringValue(ObjectId::class.java, text, INVALID_OBJECT_ID_MESSAGE) as ObjectId
        }
        return ObjectId(text)
    }
}
