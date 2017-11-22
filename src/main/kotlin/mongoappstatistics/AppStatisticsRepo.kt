package mongoappstatistics

import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

interface AppStatisticsRepo : MongoRepository<AppStatistics, String> {
    fun findByTypeAndReportTimeBetween(type: Int, startDate: Date, endDate: Date): List<AppStatistics>

}