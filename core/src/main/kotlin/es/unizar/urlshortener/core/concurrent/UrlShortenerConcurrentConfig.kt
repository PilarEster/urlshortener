package es.unizar.urlshortener.core.concurrent

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

private const val POOL_SIZE = 20
private const val CORE_POOL_SIZE = 20
private const val QUEUE_CAPACITY = 1000

/**
 * Configuration used by concurrent processes that has the anotation @Async with the content concurrentConfig
 * */
@Configuration
@EnableAsync
@EnableScheduling
open class UrlShortenerConcurrentConfig {
    @Bean("concurrentConfig")
    open fun executor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.maxPoolSize = POOL_SIZE
        executor.corePoolSize = CORE_POOL_SIZE
        executor.setQueueCapacity(QUEUE_CAPACITY)
        executor.initialize()
        return executor
    }
}
