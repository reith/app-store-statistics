package mongoappstatistics

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.ArrayList

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.*
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.*
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.*
import org.springframework.data.mongodb.core.query.Criteria

import java.io.Serializable


data class AppStatisticsWeeklyReport constructor(
        val weekNum: Int,
        val year: Int,
        var requests: Int,
        var clicks: Int,
        var installs: Int
) : Serializable, Comparable<AppStatisticsWeeklyReport> {
    override fun compareTo(other: AppStatisticsWeeklyReport) : Int = when (year) {
        other.year -> weekNum - other.weekNum
        else -> year - other.year
    }
}



interface AppStatisticsReporterService {
    fun getStats(startDate: Date, endDate: Date, type: Int) : List<AppStatisticsWeeklyReport>
}


@Service
@Profile("iso8601")
class AppStatisticsReporterNaiveImpl: AppStatisticsReporterService {

    @Autowired
    lateinit var repository: AppStatisticsRepo


    @Cacheable("weeklyappstatistics")
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
            get() {
                val reports = ArrayList(_weeklyReports.values)
                reports.sort()
                return Collections.unmodifiableList(reports)
            }

        fun addStatistics(appStats: AppStatistics): Unit {

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
    }
}

fun gregorianToPersian(gregorian: Date): Calendar {
    val cal = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
    cal.time = gregorian
    return cal
}


@Service
@Profile("!iso8601")
class AppStatisticsReporterAggregationImpl: AppStatisticsReporterService {

    @Autowired lateinit var mongoTemplate: MongoTemplate

    @Cacheable("weeklyappstatistics")
    override fun getStats(startDate: Date, endDate: Date, type: Int) : List<AppStatisticsWeeklyReport> {
        var collections = arrayListOf<List<AppStatisticsWeeklyReport>>()
        for (persianYear in gregorianToPersian(startDate).get(Calendar.YEAR)..gregorianToPersian(endDate).get(Calendar.YEAR)+1) {
            val calParams = CalendarConversionParams.forYear(persianYear, startDate, endDate)
            if (calParams != null)
                collections.add(getYearStats(type, calParams))
        }
        return collections.asSequence().flatten().toList()
    }

    private fun getYearStats(type: Int, calParams: CalendarConversionParams) : List<AppStatisticsWeeklyReport> {
        val dayOfYearExp = DateOperators.DayOfYear.dayOfYear("reportTime")
        val pastYearWoy = Ceil.ceilValueOf(
                Divide.valueOf(
                        Add.valueOf(dayOfYearExp).add(calParams.persianDayOfYearOnGregorianNewYear-1))
                .divideBy(7))
        val thisYearWoy = Ceil.ceilValueOf(
                Divide.valueOf(
                        Subtract.valueOf(dayOfYearExp).subtract(calParams.gregorianDayOfYearOnPersianNewYear-1))
                .divideBy(7))
        val pastYearCond = Cond.`when`(Lt.valueOf(dayOfYearExp)
                .lessThanValue(calParams.gregorianDayOfYearOnPersianNewYear))
        val aggregation = Aggregation.newAggregation(
                match(Criteria.where("type").`is`(type)
                        .and("reportTime").gte(calParams.startDate).lte(calParams.endDate)),
                project("videoRequests", "webViewRequests", "videoClicks", "webViewClicks", "videoInstalls", "webViewInstalls")
                        .and(pastYearCond.then(pastYearWoy).otherwise(thisYearWoy)).`as`("weekNum")
                        .and(pastYearCond.then(calParams.persianYear - 1).otherwise(calParams.persianYear)).`as`("year"),
                group("year", "weekNum")
                        .sum(Add.valueOf("videoRequests").add("webViewRequests")).`as`("requests")
                        .sum(Add.valueOf("videoInstalls").add("webViewInstalls")).`as`("installs")
                        .sum(Add.valueOf("videoClicks").add("webViewClicks")).`as`("clicks"),
                project("requests", "installs", "clicks")
                        .and("_id.year").`as`("year")
                        .and("_id.weekNum").`as`("weekNum"),
                sort(Sort.DEFAULT_DIRECTION,  "year", "weekNum")
        )
        return mongoTemplate.aggregate(aggregation, "appStatistics", AppStatisticsWeeklyReport::class.java)
                .mappedResults
    }
}

class CalendarConversionParams(
        val persianYear: Int,
        val gregorianDayOfYearOnPersianNewYear: Int,
        val persianDayOfYearOnGregorianNewYear: Int,
        val startDate: Date,
        val endDate: Date
) { companion object }


fun CalendarConversionParams.Companion.forYear(persianYear: Int, startDate: Date, endDate: Date): CalendarConversionParams? {
    val persianCal = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
    persianCal.set(persianYear, 0, 1)

    val gregorianCal = Calendar.getInstance()
    gregorianCal.time = persianCal.time

    val gregorianDayOfYearOnPersianNewYear = gregorianCal.get(Calendar.DAY_OF_YEAR)

    // now back to start of current gregorian year
    gregorianCal.set(gregorianCal.get(Calendar.YEAR), 0, 1)
    persianCal.time = gregorianCal.time
    val persianDayOfYearOnGregorianNewYear = persianCal.get(Calendar.DAY_OF_YEAR)
    println("first day of gregorian calendar overlapping this persian date: ${gregorianCal.time}")
    val rangeStartDate = if (startDate.before(gregorianCal.time)) gregorianCal.time else startDate

    // check end of year
    gregorianCal.set(Calendar.MONTH, 11)
    gregorianCal.set(Calendar.DATE, gregorianCal.getMaximum(java.util.Calendar.DATE))
    println("last day of gregorian calendar overlapping this persian date: ${gregorianCal.time}")
    val rangeEndDate = if (endDate.before(gregorianCal.time)) endDate else gregorianCal.time

    println("seeking in range $rangeStartDate $rangeEndDate")

    if (rangeStartDate.after(rangeEndDate))
        return null

    return CalendarConversionParams(
            persianYear = persianYear,
            gregorianDayOfYearOnPersianNewYear = gregorianDayOfYearOnPersianNewYear,
            persianDayOfYearOnGregorianNewYear = persianDayOfYearOnGregorianNewYear,
            startDate = rangeStartDate,
            endDate = rangeEndDate
    )
}