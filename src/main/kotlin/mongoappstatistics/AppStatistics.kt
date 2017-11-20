package mongoappstatistics

import java.util.*
import org.springframework.data.annotation.*


// TODO: it said to make properties private

data class AppStatistics(
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