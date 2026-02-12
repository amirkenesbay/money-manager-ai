package ai.moneymanager.chat.config

import ai.moneymanager.mapper.GeminiArgsMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeminiMapperConfig {

    @Bean
    fun geminiArgsMapper(): GeminiArgsMapper {
        return GeminiArgsMapper(jacksonObjectMapper())
    }
}