package mongoappstatistics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.*
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration


@Configuration
@Profile("cache")
@EnableCaching
class RedisCacheConfigurer: CachingConfigurerSupport() {

    /**
     * TTL Configurer for Redis cache
     */

    @Value("${'$'}{redis.cache.ttl:1200}")
    private lateinit var _redisCacheTtl: String

    private val redisCacheTtl: Long get() = _redisCacheTtl.toLong()

    @Autowired private lateinit var connectionFactory : RedisConnectionFactory

    @Bean
    override fun cacheManager() : RedisCacheManager {
        val cacheTtl = Duration.ofSeconds(redisCacheTtl)
        val config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(cacheTtl)
        return RedisCacheManager(RedisCacheWriter.lockingRedisCacheWriter(connectionFactory), config)
    }
}