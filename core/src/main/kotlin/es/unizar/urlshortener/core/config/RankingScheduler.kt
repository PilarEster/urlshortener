import es.unizar.urlshortener.core.usecases.RankingUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
open class RankingScheduler(
    private val rankingUseCase: RankingUseCase
) {
    @Async("executorQueueConfig")
    @Scheduled(fixedDelay = 500L)
    open
    fun executor() {
        rankingUseCase.ranking()
        rankingUseCase.user()
    }
}
