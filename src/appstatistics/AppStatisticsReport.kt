package appstatistics

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.ArrayList

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale

data class AppStatisticsWeeklyReport(
        val weekNum: Int,
        val year: Int,
        var requests: Int,
        var clicks: Int,
        var installs: Int
)


interface AppStatisticsReporterService {
    fun getStats(startDate: Date, endDate: Date, type: Int) : List<AppStatisticsWeeklyReport>
}


@Service
class AppStatisticsReporterNaiveImpl: AppStatisticsReporterService {

    @Autowired
    lateinit var repository: AppStatisticsCrud

    override fun getStats(startDate: Date, endDate: Date, type: Int) : List<AppStatisticsWeeklyReport> {
        val statistics = repository.findByTypeAndReportTimeBetween(type, startDate, endDate)
        val report = AppStatisticsReport()
        for (s in statistics) {
            report.addStatistics(s)
        }
        return report.weeklyReports
    }

    class AppStatisticsReport {
        private val _weeklyReports: MutableMap<Pair<Int, Int>, AppStatisticsWeeklyReport> = mutableMapOf()
        val weeklyReports: List<AppStatisticsWeeklyReport>
            get() = Collections.unmodifiableList(ArrayList(_weeklyReports.values))

        fun addStatistics(appStats: AppStatistics) : Unit {

            val persianCalDate = gregorianToPersian(appStats.reportTime)
            val persianYear = persianCalDate.get(Calendar.YEAR)
            val persianWeekNum = persianCalDate.get(Calendar.WEEK_OF_YEAR)
            _weeklyReports.putIfAbsent(Pair(persianYear, persianWeekNum),
                    AppStatisticsWeeklyReport(weekNum = persianWeekNum, year = persianYear, requests = 0, clicks = 0, installs = 0))
            val weekStats = _weeklyReports.getOrDefault(Pair(persianYear, persianWeekNum),
                    AppStatisticsWeeklyReport(persianYear, persianWeekNum, 0, 0, 0))
            weekStats.requests += appStats.videoRequests + appStats.webViewRequests
            weekStats.clicks += appStats.videoClicks + appStats.webViewClicks
            weekStats.installs += appStats.videoInstalls + appStats.webViewInstalls
        }
    }
}

fun gregorianToPersian(gregorian: Date): Calendar {
    val cal = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
    cal.time = gregorian
    return cal
}