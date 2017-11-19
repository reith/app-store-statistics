package appstatistics

import org.h2.tools.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.*

@Configuration
@Profile("dev")
class H2ServerConfiguration {
    @Value("${'$'}{h2.web.port:8092}")
    private lateinit var h2WebPort: String

    @Bean
    @ConditionalOnExpression("${'$'}{h2.web.enabled:true}")
    fun h2WebServer() : Server =
         Server.createWebServer("-web", "-webAllowOthers", "-webPort", h2WebPort).start()
}