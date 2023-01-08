package es.unizar.urlshortener.core.usecases

import org.springframework.http.HttpStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.BlockingQueue

// import java.time.Duration

// private const val REQUEST_HEAD_TIMEOUT = 2L
private const val UPDATE_REACHABILITY_TIMEOUT = 5L

/**
 * Class that manages the reachability info of the stored URLs.
 *
 * *[reach]* checks the reachability of a URL.
 *
 * *[isReachable]* provides reachability info of a URL.
 *
 * *[updateReachableUrl]* check if stored reachability info must be updated.
 *
 * **Note**: To provide reachability info it must be checked previously.
 */
interface ReachableWebUseCase {
    fun reach(url: String)

    fun isReachable(url: String): Boolean?

    fun updateReachableUrl()
}

/**
 * Implementation of [ReachableWebUseCase].
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
class ReachableWebUseCaseImpl(
    private val reachableMap: HashMap<String, Pair<Boolean, OffsetDateTime>>,
    private val reachableQueue: BlockingQueue<String>
) : ReachableWebUseCase {
    override fun reach(url: String) {
        val client = HttpClient.newBuilder().build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("HEAD", BodyPublishers.noBody())
            .build()

        val response = client.send(request, BodyHandlers.discarding())

        if (HttpStatus.valueOf(response.statusCode()).is4xxClientError ||
            HttpStatus.valueOf(response.statusCode()).is5xxServerError
        ) {
            reachableMap.put(url, Pair(false, OffsetDateTime.now()))
        } else {
            reachableMap.put(url, Pair(true, OffsetDateTime.now()))
        }
    }

    override fun isReachable(url: String): Boolean? = reachableMap.get(url)?.first

    override fun updateReachableUrl() {
        reachableMap.map { i ->
            if (i.value.second.until(OffsetDateTime.now(), ChronoUnit.SECONDS) > UPDATE_REACHABILITY_TIMEOUT) {
                reachableQueue.put(i.key)
            }
        }
    }
}
