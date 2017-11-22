package mongoappstatistics

import java.util.*
import org.springframework.data.annotation.*


data class AppStatistics(
        /**
         * Some info about application
         *
         * @param reportTime: report time in gregorian calendar
         */
        val reportTime: Date,
        val type: Int,
        val videoRequests: Int,
        val webViewRequests: Int,
        val videoClicks: Int,
        val webViewClicks: Int,
        val videoInstalls: Int,
        val webViewInstalls: Int
) {
    @Id
    val id: String? = null
}