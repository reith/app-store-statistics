package appstatistics

import java.util.*
import javax.persistence.*


// TODO: it said to make properties private


@Entity
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
    // FIXME: ID should be String
    @GeneratedValue @Id val id: Int? = null
}