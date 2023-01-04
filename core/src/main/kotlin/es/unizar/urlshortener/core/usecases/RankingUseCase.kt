package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Interface to recover url data from SQL query
 * */
interface ClickSum {
    fun getHash(): String
    fun getSum(): Int
}

/**
 * Interface to recover user data from SQL query
 * */
interface ClickUserSum {
    fun getIp(): String
    fun getSum(): Int
}

/**
 * Explicit class to store url data from SQL query through [ClickSUm] interface
 * */
data class UrlSum(
    var hash: String = "",
    var sum: Int
)

/**
 * Explicit class to store user data from SQL query through [ClickUserSUm] interface
 * */
data class UserSum(
    var ip: String,
    var sum: Int
)

/**
 * Log that somebody has requested the lait of urls or the list of users
 */
interface RankingUseCase {
    fun ranking(): List<UrlSum>
    fun user(): List<UserSum>
}

/**
 * Implementation of [RankingUseCase]
 */
class RankingUseCaseImpl(
    private val shortUrlRepositoryService: ShortUrlRepositoryService,
    private val clickRepositoryService: ClickRepositoryService
) : RankingUseCase {
    override fun ranking(): List<UrlSum> {
        var resp = clickRepositoryService.computeClickSum().map { case ->
            shortUrlRepositoryService.findByKey(case.getHash()).let { shortUrl ->
                if (shortUrl != null) {
                    UrlSum(shortUrl.hash, case.getSum())
                } else null
            }
        }.filterNotNull()
        return resp.sortedByDescending { it.sum }
        return resp
    }
    override fun user(): List<UserSum> {
        var lista = shortUrlRepositoryService.computeUserClicks().map { case ->
            UserSum(case.getIp(), case.getSum())
        }
        return lista.sortedByDescending { it.sum }
    }
}
