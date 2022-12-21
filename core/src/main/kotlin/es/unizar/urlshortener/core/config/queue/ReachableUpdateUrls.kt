package es.unizar.urlshortener.core.config.queue

import es.unizar.urlshortener.core.usecases.ReachableWebUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
open class ReachableUpdateUrls(
    private val reachableWebUseCase: ReachableWebUseCase
) {
    @Async("executorQueueConfig")
    @Scheduled(fixedDelay = 5000L)
    open
    fun executor() {
        reachableWebUseCase.updateReachableUrl()
    }
}
