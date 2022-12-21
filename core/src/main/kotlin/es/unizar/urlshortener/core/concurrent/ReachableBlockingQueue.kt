package es.unizar.urlshortener.core.concurrent

import es.unizar.urlshortener.core.usecases.ReachableWebUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue

@Component
open class ReachableBlockingQueue(
    private val reachableQueue: BlockingQueue<String>,
    private val reachableWebUseCase: ReachableWebUseCase
) {
    @Async("executorQueueConfig")
    @Scheduled(fixedDelay = 500L)
    open
    fun executor() {
        if (!reachableQueue.isEmpty()) {
            val result = reachableQueue.take()
            reachableWebUseCase.reach(result)
        }
    }
}
