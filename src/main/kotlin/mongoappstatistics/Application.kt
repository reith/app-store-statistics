package mongoappstatistics

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@SpringBootApplication
class Application {

    private val log: Logger = LoggerFactory.getLogger(Application::class.java)

    @Bean
    fun demo(repository: AppStatisticsRepo, service: AppStatisticsReporterService) = CommandLineRunner {
        log.info("Saving mocking statistics")
        val d = LocalDateTime.now()
        val dy = d.plusDays(-1)
        val dt = d.plusDays(2)

        var startDate = Date.from(dy.atZone(ZoneId.systemDefault()).toInstant())
        var endDate = Date.from(dt.atZone(ZoneId.systemDefault()).toInstant())
        var now = Date.from(d.atZone(ZoneId.systemDefault()).toInstant())
        
        repository.save(AppStatistics(now, 0, 2, 3, 4, 5, 6, 7))
        repository.save(AppStatistics(now, 0, 2, 3, 4, 5, 6, 7))

        val stats = service.getStats(startDate, endDate, 0)
        println("Applications Weekly Stats: $stats")

    }

}

fun main(args: Array<String>) {
    println("Starting mongoappstatistics")
    SpringApplication.run(Application::class.java, *args).close()
}
