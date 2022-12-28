package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.UrlNotSafe
import es.unizar.urlshortener.core.WebUnreachable
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.QrCodeUseCase
import es.unizar.urlshortener.core.usecases.RankingUseCase
import es.unizar.urlshortener.core.usecases.ReachableWebUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.util.concurrent.BlockingQueue

@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class
    ]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @Suppress("UnusedPrivateMember")
    @MockBean
    private lateinit var rankingUseCase: RankingUseCase

    @MockBean
    private lateinit var qrCodeUseCase: QrCodeUseCase

    @MockBean
    private lateinit var reachableWebUseCase: ReachableWebUseCase

    @Suppress("UnusedPrivateMember")
    @MockBean
    private lateinit var qrQueue: BlockingQueue<Pair<String, String>>

    @Suppress("UnusedPrivateMember")
    @MockBean
    private lateinit var reachableQueue: BlockingQueue<String>

    @Test
    fun `redirectTo returns a redirect when the key exists, is safe and reachable`() {
        given(redirectUseCase.redirectTo("key"))
            .willReturn(
                ShortUrl(
                    "",
                    Redirection("http://example.com/"),
                    created = OffsetDateTime.now(),
                    ShortUrlProperties(safe = true)
                )
            )

        given(reachableWebUseCase.isReachable("http://example.com/")).willReturn(true)
        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a bad request with retry-after when the key exists and safety is unknown`() {
        given(redirectUseCase.redirectTo("key"))
            .willReturn(
                ShortUrl(
                    "",
                    Redirection("http://example.com/health")
                )
            )

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isBadRequest)
            .andExpect(header().stringValues("Retry-After", "500"))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a forbidden when the key exists and is not safe`() {
        given(redirectUseCase.redirectTo("key"))
            .willReturn(
                ShortUrl(
                    "",
                    Redirection("http://example.com/health"),
                    created = OffsetDateTime.now(),
                    ShortUrlProperties(safe = false)
                )
            )

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isForbidden)

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a bad request when the key exists and the website is unreachable`() {
        given(redirectUseCase.redirectTo("key"))
            .willReturn(
                ShortUrl(
                    "",
                    Redirection("http://example.com/health"),
                    created = OffsetDateTime.now(),
                    ShortUrlProperties(safe = true)
                )
            )

        given(
            reachableWebUseCase.reach("http://example.com/health")
        ).willAnswer { throw WebUnreachable("http://example.com/health") }

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isBadRequest)

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("qr", "false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash with qr`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", qr = true)
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("qr", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(
                content().json(
                    """
                        {
                          "url": "http://localhost/f684a3c4",
                          "properties": {
                            "qr": "http://localhost/f684a3c4/qr"
                          }
                        }
                    """.trimIndent()
                )
            )
    }

    @Test
    fun `creates returns a forbidden if the url is shortened yet and is not secure`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willAnswer { throw UrlNotSafe("http://example.com/") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("qr", "false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .param("qr", "false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `qr returns an image when the key exists`() {
        given(qrCodeUseCase.getQR("key")).willReturn("Hello".toByteArray())

        mockMvc.perform(get("/{id}/qr", "key"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes("Hello".toByteArray()))
    }

    @Test
    fun `qr returns a not found when the key does not exist`() {
        given(qrCodeUseCase.getQR("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}/qr", "key"))
            .andDo(print())
    }
}
