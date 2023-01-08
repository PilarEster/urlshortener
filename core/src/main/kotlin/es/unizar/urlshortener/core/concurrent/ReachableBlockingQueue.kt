package es.unizar.urlshortener.core.concurrent

import es.unizar.urlshortener.core.usecases.ReachableWebUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue

/**
 * Concurrent process that check the reachability of a URL following a queue order.
 *
 * To manage the concurrence, it's being using a [BlockingQueue].
 */
@Component
open class ReachableBlockingQueue(
    private val reachableQueue: BlockingQueue<String>,
    private val reachableWebUseCase: ReachableWebUseCase
) {
    @Async("concurrentConfig")
    @Scheduled(fixedDelay = 500L)
    open
    fun executor() {
        if (!reachableQueue.isEmpty()) {
            val result = reachableQueue.take()
            reachableWebUseCase.reach(result)
        }
    }
}
