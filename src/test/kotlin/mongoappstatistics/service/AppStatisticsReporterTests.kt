package mongoappstatistics.service

import mongoappstatistics.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import org.assertj.core.api.Assertions.assertThat


@RunWith(SpringRunner::class)
@ComponentScan("mongoappstatistics")
@TestPropertySource(properties = arrayOf("spring.data.mongodb.database=appStatsTest"))
@SpringBootTest
class AppStatisticsReporterTests {

    @Autowired lateinit var repository: AppStatisticsRepo

    @Autowired lateinit var service: AppStatisticsReporterService

    @Before
    @After
    fun clearAppStatistics() {
        repository.deleteAll()
    }

    @Test
    fun singleDayTest() {
        val now = LocalDateTime.now()
        val nowDate = now.toDate()

        repository.save(AppStatistics(nowDate, 0, 2, 3, 4, 5, 6, 7))
        repository.save(AppStatistics(nowDate, 1, 4, 7, 1,0,2,6))
        repository.save(AppStatistics(nowDate, 0,3,2,6,0,1,3))

        val stats = service.getStats(now.minusDays(400).toDate(),
                now.plusDays(2).toDate(), 0)
        assertThat(stats.isNotEmpty() && stats[0].requests == 10 && stats[0].clicks == 15 && stats[0].installs == 17).isTrue()
    }

    @Test
    fun oneWeekDiffTest() {
        val d = LocalDateTime.of(2016, 2,5,0,0,0)

        val newer = d.toDate()
        val older = d.minusDays(7).toDate()

        repository.save(AppStatistics(newer, 0,2,3,2,1,3,2))
        repository.save(AppStatistics(older,0,2,1,2,1,4,2))

        val stats = service.getStats(d.minusDays(8).toDate(),
                d.plusSeconds(1).toDate(), 0)
        assertThat(stats.size).isEqualTo(2)
    }

    @Test
    fun boundryRangeExclusionTest() {
        val d = LocalDateTime.now()
        val nowDate = d.toDate()

        repository.save(AppStatistics(nowDate,0,3,2,2,2,1,0))

        val statsLE = service.getStats(nowDate, d.plusSeconds(1).toDate(), 0)
        assertThat(statsLE).isEmpty()

        val statsGE = service.getStats(d.minusSeconds(1).toDate(), nowDate, 0)
        assertThat(statsGE).isEmpty()
    }
}


fun LocalDateTime.toDate() : Date = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())