package mongoappstatistics

import java.io.Serializable


data class AppStatisticsWeeklyReport constructor(
        /**
         * Accumulated statistics of applications registered in one week.
         *
         * @param weekNum: week number (ISO or plain, differs based on implementation) in persian calendar
         * @param year: year corresponding to week number in persian calendar
         * @param requests: sum of applications' videoRequests and webViewRequests
         * @param clicks: sum of applications' videoClicks and webViewClicks
         * @param installs: sum of applications' videoInstalls and webViewInstalls
         */
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