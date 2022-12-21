package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.WebUnreachable
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

interface ReachableWebUseCase {
    fun reach(url: String)

    fun isReachable(url: String): Boolean

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

        try {
            val response = client.send(request, BodyHandlers.discarding())

            if (response.statusCode() >= HttpStatus.BAD_REQUEST.value()) {
                reachableMap.put(url, Pair(false, OffsetDateTime.now()))
                throw WebUnreachable(url)
            }
            reachableMap.put(url, Pair(true, OffsetDateTime.now()))
        } catch (cause: Throwable) {
            throw WebUnreachable(url)
        }
    }

    override fun isReachable(url: String): Boolean =
        reachableMap.get(url)?.first ?: throw WebUnreachable(url)

    override fun updateReachableUrl() {
        reachableMap.map { i ->
            if (i.value.second.until(OffsetDateTime.now(), ChronoUnit.SECONDS) > UPDATE_REACHABILITY_TIMEOUT) {
                reachableQueue.put(i.key)
            }
        }
    }
}
