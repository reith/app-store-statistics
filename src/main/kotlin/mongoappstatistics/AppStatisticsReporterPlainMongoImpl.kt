package mongoappstatistics

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import jdk.nashorn.internal.runtime.regexp.joni.Config.log
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.util.*

@Service
@Profile("!iso8601")
class AppStatisticsReporterPlainMongoImpl: AppStatisticsReporterService {
    /**
     * AppStatistics reporter based on plain week numbering as implemented by MongoDB
     *
     * In this system each year has exactly 53 weeks and it starts at new year beginning. Last week of year has less
     * than seven days and days of year are in weeks belonging to same year.
     *
     * @note this needs MongoDB 2.6 and newer
     */

    @Autowired lateinit var mongoTemplate: MongoTemplate

    @Cacheable("weeklyAppStats")
    override fun getStats(startDate: Date, endDate: Date, type: Int) : List<AppStatisticsWeeklyReport> =
            queryAndMergeYearlyStatistics(startDate, endDate, type)


    private fun queryAndMergeYearlyStatistics(startDate: Date, endDate: Date, type: Int): List<AppStatisticsWeeklyReport> {
        val collections = arrayListOf<List<AppStatisticsWeeklyReport>>()
        for (year in gregorianToPersian(startDate).get(Calendar.YEAR)..gregorianToPersian(endDate).get(Calendar.YEAR)+1) {
            val calParams = CalendarConversionParams.forYear(year, startDate, endDate)
            if (calParams != null) {
                var newStats = getYearStats(type, calParams)
                if (! collections.isEmpty() && !newStats.isEmpty() && !collections.last().isEmpty()) {
                    val prevLast = collections.last().last()
                    val curFist = newStats.first()
                    if (prevLast.year == curFist.year && prevLast.weekNum == curFist.weekNum) {
                        prevLast.clicks += curFist.clicks
                        prevLast.installs += curFist.installs
                        prevLast.requests += curFist.requests
                        newStats = newStats.drop(1)
                    }
                }
                collections.add(newStats)
            }
        }
        return collections.asSequence().flatten().toList()
    }

    private fun getYearStats(type: Int, calParams: CalendarConversionParams) : List<AppStatisticsWeeklyReport> {
        /**
         * Get accumulated application statistics in at-most one full gregorian year. Output year and week number
         * of this method is in persian calendar
         */
        val dayOfYearExp = DateOperators.DayOfYear.dayOfYear("reportTime")
        val pastYearWoy = ArithmeticOperators.Ceil.ceilValueOf(
                ArithmeticOperators.Divide.valueOf(
                        ArithmeticOperators.Add.valueOf(dayOfYearExp).add(calParams.persianDayOfYearOnGregorianNewYear-1))
                        .divideBy(7))
        val thisYearWoy = ArithmeticOperators.Ceil.ceilValueOf(
                ArithmeticOperators.Divide.valueOf(
                        ArithmeticOperators.Subtract.valueOf(dayOfYearExp).subtract(calParams.gregorianDayOfYearOnPersianNewYear-1))
                        .divideBy(7))
        val pastYearCond = ConditionalOperators.Cond.`when`(ComparisonOperators.Lt.valueOf(dayOfYearExp)
                .lessThanValue(calParams.gregorianDayOfYearOnPersianNewYear))
        val aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("type").`is`(type)
                        .and("reportTime").gt(calParams.startDate).lt(calParams.endDate)),
                Aggregation.project("videoRequests", "webViewRequests", "videoClicks", "webViewClicks", "videoInstalls", "webViewInstalls")
                        .and(pastYearCond.then(pastYearWoy).otherwise(thisYearWoy)).`as`("weekNum")
                        .and(pastYearCond.then(calParams.persianYear - 1).otherwise(calParams.persianYear)).`as`("year"),
                Aggregation.group("year", "weekNum")
                        .sum(ArithmeticOperators.Add.valueOf("videoRequests").add("webViewRequests")).`as`("requests")
                        .sum(ArithmeticOperators.Add.valueOf("videoInstalls").add("webViewInstalls")).`as`("installs")
                        .sum(ArithmeticOperators.Add.valueOf("videoClicks").add("webViewClicks")).`as`("clicks"),
                Aggregation.project("requests", "installs", "clicks")
                        .and("_id.year").`as`("year")
                        .and("_id.weekNum").`as`("weekNum"),
                Aggregation.sort(Sort.DEFAULT_DIRECTION, "year", "weekNum")
        )
        return mongoTemplate.aggregate(aggregation, AppStatistics::class.java, AppStatisticsWeeklyReport::class.java)
                .mappedResults
    }


    class CalendarConversionParams(
            /**
             * A placeholder for variables needed to extract Persian calendar week numbers from Gregorian date
             */
            val persianYear: Int,
            val gregorianDayOfYearOnPersianNewYear: Int,
            val persianDayOfYearOnGregorianNewYear: Int,
            val startDate: Date,
            val endDate: Date
    ) {
        companion object {

            fun forYear(persianYear: Int, startDate: Date, endDate: Date): CalendarConversionParams? {
                val persianCal = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
                persianCal.set(persianYear, 0, 1, 0,0,0)

                val gregorianCal = Calendar.getInstance()
                gregorianCal.time = persianCal.time

                val gregorianDayOfYearOnPersianNewYear = gregorianCal.get(Calendar.DAY_OF_YEAR)

                // now back to start of current gregorian year
                gregorianCal.set(gregorianCal.get(Calendar.YEAR), 0, 1, 0, 0,0)
                persianCal.time = gregorianCal.time
                val persianDayOfYearOnGregorianNewYear = persianCal.get(Calendar.DAY_OF_YEAR)
                val rangeStartDate = if (startDate.before(gregorianCal.time)) gregorianCal.time else startDate

                // check end of year
                for (field in listOf(Calendar.MONTH, Calendar.DATE, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND)) {
                    gregorianCal.set(field, gregorianCal.getMaximum(field))
                }
                val rangeEndDate = if (endDate.before(gregorianCal.time)) endDate else gregorianCal.time

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
        }
    }
}
