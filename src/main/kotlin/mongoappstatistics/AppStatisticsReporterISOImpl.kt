package mongoappstatistics

import com.ibm.icu.util.Calendar
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.*


@Service
@Profile("iso8601")
class AppStatisticsReporterISOImpl: AppStatisticsReporterService {
    /**
     * AppStatistics reporter based on ISO-8601 week number definition.
     *
     * In this system weeks numbered in cycle of years, thus every week has exactly seven days. It's usual that some
     * days of weeks around new year belong to week of last or next year; in this case year is corresponding year to
     * week number.
     *
     * @see https://en.wikipedia.org/wiki/ISO_week_date
     */

    @Autowired
    lateinit var repository: AppStatisticsRepo

    @Cacheable("weeklyAppStats")
    override fun getStats(startDate: Date, endDate: Date, type: Int) : List<AppStatisticsWeeklyReport> {
        val statistics = repository.findByTypeAndReportTimeBetween(type, startDate, endDate)
        return AppStatisticsReport.join(statistics).weeklyReports
    }

    class AppStatisticsReport private constructor() {
        private val _weeklyReports: MutableMap<Pair<Int, Int>, AppStatisticsWeeklyReport> = mutableMapOf()

        val weeklyReports: List<AppStatisticsWeeklyReport>
            get() {
                val reports = ArrayList(_weeklyReports.values)
                reports.sort()
                return Collections.unmodifiableList(reports)
            }

        private fun addStatistics(appStats: AppStatistics): Unit {
            val persianCalDate = gregorianToPersian(appStats.reportTime)
            val persianYear = persianCalDate.get(Calendar.YEAR_WOY)
            val persianWeekNum = persianCalDate.get(Calendar.WEEK_OF_YEAR)
            _weeklyReports.putIfAbsent(Pair(persianYear, persianWeekNum),
                    AppStatisticsWeeklyReport(weekNum = persianWeekNum, year = persianYear, requests = 0, clicks = 0, installs = 0))
            val weekStats = _weeklyReports.getOrDefault(Pair(persianYear, persianWeekNum),
                    AppStatisticsWeeklyReport(persianYear, persianWeekNum, 0, 0, 0))
            weekStats.requests += appStats.videoRequests + appStats.webViewRequests
            weekStats.clicks += appStats.videoClicks + appStats.webViewClicks
            weekStats.installs += appStats.videoInstalls + appStats.webViewInstalls
        }
        companion object {
            fun join(statistics: List<AppStatistics>) : AppStatisticsReport {
                val report = AppStatisticsReport()
                for (s in statistics) {
                    report.addStatistics(s)
                }
                return report
            }
        }
    }
}