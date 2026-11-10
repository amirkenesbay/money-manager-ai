package ai.moneymanager.repository

import ai.moneymanager.repository.entity.KztRatesEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface KztRatesRepository : MongoRepository<KztRatesEntity, String> {
    fun findFirstByDayLessThanEqualOrderByDayDesc(day: String): KztRatesEntity?
}
