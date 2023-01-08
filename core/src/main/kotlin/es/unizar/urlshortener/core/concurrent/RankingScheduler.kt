package es.unizar.urlshortener.core.concurrent

import es.unizar.urlshortener.core.usecases.RankingUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Concurrent process that generates the ranking of URLs and users.
 */
@Component
open class RankingScheduler(
    private val rankingUseCase: RankingUseCase
) {
    @Async("concurrentConfig")
    @Scheduled(fixedDelay = 500L)
    open
    fun executor() {
        rankingUseCase.ranking()
        rankingUseCase.user()
    }
}
