package appstatistics

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.sql.Date
import java.time.LocalDate


@SpringBootApplication
class Application {

    private val log: Logger = LoggerFactory.getLogger(Application::class.java)

    @Bean
    fun demo(repository: AppStatisticsCrud, service: AppStatisticsReporterService) = CommandLineRunner {
        log.info("Saving mocking statistics")
        val d = LocalDate.now()



        repository.save(AppStatistics(Date.valueOf(d), 0, 2, 3, 4, 5, 6, 7))
        repository.save(AppStatistics(Date.valueOf(d), 0, 2, 3, 4, 5, 6, 7))

        val stats = service.getStats(java.sql.Date.valueOf(d.plusDays(-1)), java.sql.Date.valueOf(d), 0)
        println("Applications Weekly Stats: $stats")
    }

}


/*
fun main(args: Array<String>) {
    println("Starting appstatistics.AppStatistics")
    SpringApplication.run(Application::class.java, *args)
}
*/
