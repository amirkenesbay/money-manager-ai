package ai.moneymanager.chat.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Configuration
@EnableMongoAuditing
class MongoConfig {
    @Bean
    fun customConversions(): MongoCustomConversions {
        val converterList = listOf(
            LocalDateTimeToDateConverter(),
            DateToLocalDateTimeConverter()
        )
        return MongoCustomConversions(converterList)
    }
}

@WritingConverter
class LocalDateTimeToDateConverter : Converter<LocalDateTime, Date> {
    override fun convert(source: LocalDateTime): Date {
        return Date.from(source.atZone(ZoneId.systemDefault()).toInstant())
    }
}

@ReadingConverter
class DateToLocalDateTimeConverter : Converter<Date, LocalDateTime> {
    override fun convert(source: Date): LocalDateTime {
        return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}