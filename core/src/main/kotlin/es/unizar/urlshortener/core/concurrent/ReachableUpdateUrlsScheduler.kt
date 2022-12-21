package es.unizar.urlshortener.core.concurrent

import es.unizar.urlshortener.core.usecases.ReachableWebUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
open class ReachableUpdateUrlsScheduler(
    private val reachableWebUseCase: ReachableWebUseCase
) {
    @Async("executorQueueConfig")
    @Scheduled(fixedDelay = 5000L)
    open
    fun executor() {
        reachableWebUseCase.updateReachableUrl()
    }
}
