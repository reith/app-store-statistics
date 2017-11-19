package appstatistics

import org.springframework.data.repository.CrudRepository
import java.util.*

interface AppStatisticsCrud : CrudRepository<AppStatistics, String> {
    fun findByType(type: Int) : List<AppStatistics>
    fun findByTypeAndReportTimeBetween(type: Int, startDate: Date, endDate: Date): List<AppStatistics>
}