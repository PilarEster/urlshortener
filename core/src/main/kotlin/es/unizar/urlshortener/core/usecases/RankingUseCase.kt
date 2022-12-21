package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrlRepositoryService

interface ClickSum {
    fun getHash(): String
    fun getSum(): Int
}

interface ClickUserSum {
    fun getIp(): String
    fun getSum(): Int
}

data class UrlSum(
    var hash: String = "",
    var sum: Int
)

data class UserSum(
    var ip: String,
    var sum: Int
)

interface RankingUseCase {
    fun ranking(): List<UrlSum>
    fun user(): List<UserSum>
}

/**
 * Implementation of [RankingUseCase].
 */
class RankingUseCaseImpl(
    private val shortUrlRepositoryService: ShortUrlRepositoryService,
    private val clickRepositoryService: ClickRepositoryService
) : RankingUseCase {
    override fun ranking(): List<UrlSum> =
        clickRepositoryService.computeClickSum().map { case ->
            shortUrlRepositoryService.findByKey(case.getHash()).let { shortUrl ->
                if (shortUrl != null) {
                    UrlSum(shortUrl.hash, case.getSum())
                } else null
            }
        }.filterNotNull()

    override fun user(): List<UserSum> =
        shortUrlRepositoryService.computeUserClicks().map { case ->
            UserSum(case.getIp(), case.getSum())
        }
}
