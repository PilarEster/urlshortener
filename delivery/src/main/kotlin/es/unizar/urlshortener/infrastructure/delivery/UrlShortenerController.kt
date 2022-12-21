package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.QrCodeUseCase
import es.unizar.urlshortener.core.usecases.RankingUseCase
import es.unizar.urlshortener.core.usecases.ReachableWebUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.UrlSum
import es.unizar.urlshortener.core.usecases.UserSum
import org.springframework.core.io.ByteArrayResource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.concurrent.BlockingQueue
import javax.servlet.http.HttpServletRequest

private const val RETRY_AFTER_DELAY = 500L

/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    fun ranking(request: HttpServletRequest): ResponseEntity<RankingDataOut>

    fun users(request: HttpServletRequest): ResponseEntity<UserDataOut>

    fun generateQrCode(id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qr: Boolean
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * Data returned after the creation of a ranking.
 */
data class RankingDataOut(
    val list: List<UrlSum> = emptyList()
)

data class UserDataOut(
    val list: List<UserSum> = emptyList()
)

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@Suppress("LongParameterList")
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val qrCodeUseCase: QrCodeUseCase,
    val rankingUseCase: RankingUseCase,
    val reachableWebUseCase: ReachableWebUseCase,
    val qrQueue: BlockingQueue<Pair<String, String>>,
    val reachableQueue: BlockingQueue<String>
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> {

        redirectUseCase.redirectTo(id).let { redirect ->
            if (reachableWebUseCase.isReachable(redirect.target)) {
                logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
                val h = HttpHeaders()
                h.location = URI.create(redirect.target)
                return ResponseEntity<Void>(h, HttpStatus.valueOf(redirect.mode))
            } else {
                val h = HttpHeaders()
                h.location = URI.create(redirect.target)
                h.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_DELAY.toString())
                return ResponseEntity<Void>(h, HttpStatus.BAD_REQUEST)
            }
        }
    }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =

        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                qr = data.qr
            )
        ).let {
            println(request.remoteAddr)
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url

            if (data.qr) qrQueue.put(Pair(it.hash, url.toString()))
            reachableQueue.put(data.url)

            val response = ShortUrlDataOut(
                url = url,
                properties = when (data.qr) {
                    false -> mapOf(
                        "safe" to it.properties.safe
                    )
                    true -> mapOf(
                        "safe" to it.properties.safe,
                        "qr" to linkTo<UrlShortenerControllerImpl> { generateQrCode(it.hash, request) }.toUri()
                    )
                }
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/{id:(?!api|index).*}/qr")
    override fun generateQrCode(
        @PathVariable id: String,
        request: HttpServletRequest
    ): ResponseEntity<ByteArrayResource> =

        qrCodeUseCase.getQR(id).let {
            val headers = HttpHeaders()
            headers.set(HttpHeaders.CONTENT_TYPE, IMAGE_PNG_VALUE)
            ResponseEntity<ByteArrayResource>(ByteArrayResource(it, IMAGE_PNG_VALUE), headers, HttpStatus.OK)
        }

    @GetMapping("/api/link")
    override fun ranking(request: HttpServletRequest): ResponseEntity<RankingDataOut> =
        rankingUseCase.ranking().let {
            val response = RankingDataOut(
                list = it
            )
            ResponseEntity<RankingDataOut>(response, HttpStatus.OK)
        }

    @GetMapping("/api/link/{id}")
    override fun users(request: HttpServletRequest): ResponseEntity<UserDataOut> =
        rankingUseCase.user().let {
            val response = UserDataOut(
                list = it
            )
            ResponseEntity<UserDataOut>(response, HttpStatus.OK)
        }
}
