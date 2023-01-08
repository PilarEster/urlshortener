package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.UrlNotSafe
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.QrCodeUseCase
import es.unizar.urlshortener.core.usecases.RankingUseCase
import es.unizar.urlshortener.core.usecases.ReachableWebUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.UrlSum
import es.unizar.urlshortener.core.usecases.UserSum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

    /**
     * Creates a ranking of urls.
     */
    fun ranking(request: HttpServletRequest): ResponseEntity<RankingDataOut>

    /**
     * Creates a ranking of users.
     */
    fun users(request: HttpServletRequest): ResponseEntity<UserDataOut>

    /**
     * Provides a QR Code identified by its [id].
     */
    fun generateQrCode(id: String, request: HttpServletRequest): ResponseEntity<ByteArrayResource>
}

/**
 * Data required to create a short url.
 */
@Schema(description = "Input data for /api/link")
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qr: Boolean
)

/**
 * Data returned after the creation of a short url.
 */
@Schema(description = "Output data for /api/link")
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * Data returned after the creation of a url ranking.
 */
@Schema(description = "Output data for /api/link/urls")
data class RankingDataOut(
    val list: List<UrlSum> = emptyList()
)

/**
 * Data returned after the creation of a user ranking.
 */
@Schema(description = "Output data for /api/link/users")
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

    @Operation(
        summary = "Redirect to URL identified by id",
        description = "Given an id, redirects if it's possible to the web associated"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "307",
                description = "Redirection successfully"
            ),
            ApiResponse(
                responseCode = "400",
                description = "Safety unknown or destination not reachable"
            ),
            ApiResponse(
                responseCode = "403",
                description = "destination is not safety"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Id doesn't exists"
            )
        ]
    )
    @Suppress("NestedBlockDepth", "ReturnCount")
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> {

        redirectUseCase.redirectTo(id).let { shorturl ->

            shorturl.properties.safe?.let { safe ->
                if (safe) {
                    reachableWebUseCase.isReachable(shorturl.redirection.target)?.let { reachable ->
                        if (reachable) {
                            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
                            val h = HttpHeaders()
                            h.location = URI.create(shorturl.redirection.target)
                            return ResponseEntity<Void>(h, HttpStatus.valueOf(shorturl.redirection.mode))
                        } else {
                            val h = HttpHeaders()
                            h.location = URI.create(shorturl.redirection.target)
                            h.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_DELAY.toString())
                            return ResponseEntity<Void>(h, HttpStatus.BAD_REQUEST)
                        }
                    } ?: run {
                        val h = HttpHeaders()
                        h.location = URI.create(shorturl.redirection.target)
                        h.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_DELAY.toString())
                        return ResponseEntity<Void>(h, HttpStatus.BAD_REQUEST)
                    }
                } else {
                    throw UrlNotSafe(shorturl.redirection.target)
                }
            } ?: run {
                val h = HttpHeaders()
                h.location = URI.create(shorturl.redirection.target)
                h.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_DELAY.toString())
                return ResponseEntity<Void>(h, HttpStatus.BAD_REQUEST)
            }
        }
    }

    @Operation(
        summary = "Creates a shortened URL",
        description = "Given an url, returns a shortened URL, optionally can generate a QR Code"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "URL shortened successfully"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Try to short a known unsafe URL"
            )
        ]
    )
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
                    false -> mapOf()
                    true -> mapOf(
                        "qr" to linkTo<UrlShortenerControllerImpl> { generateQrCode(it.hash, request) }.toUri()
                    )
                }
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @Operation(
        summary = "Return a QR Code identified by id",
        description = "Given an id, returns a QR Code (if it's possible) in PNG format"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returns QR Code"
            ),
            ApiResponse(
                responseCode = "400",
                description = "QR not generated yet or id doesn't exists"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Id doesn't exists"
            )
        ]
    )
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

    @Operation(
        summary = "Return a ranking of the most used short URLs"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returns ranking of URLs"
            )
        ]
    )
    @GetMapping("/api/link/urls")
    override fun ranking(request: HttpServletRequest): ResponseEntity<RankingDataOut> =
        rankingUseCase.ranking().let {
            val response = RankingDataOut(
                list = it
            )
            ResponseEntity<RankingDataOut>(response, HttpStatus.OK)
        }

    @Operation(
        summary = "Return a ranking of the users that generate more URLs"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returns ranking of users"
            )
        ]
    )
    @GetMapping("/api/link/users")
    override fun users(request: HttpServletRequest): ResponseEntity<UserDataOut> =
        rankingUseCase.user().let {
            val response = UserDataOut(
                list = it
            )
            ResponseEntity<UserDataOut>(response, HttpStatus.OK)
        }
}
