package mongoappstatistics

import java.util.*

/**
 * Query facility for application statistics
 */
interface AppStatisticsReporterService {

    /**
     * Get statistics of applications reported between @param startDate and @param endDate and of type @param type
     */
    fun getStats(startDate: Date, endDate: Date, type: Int) : List<AppStatisticsWeeklyReport>

}