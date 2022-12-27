package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.net.URI
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setup() {
        val httpClient = HttpClientBuilder.create()
            .disableRedirectHandling()
            .build()
        (restTemplate.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory).httpClient = httpClient

        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @Test
    fun `main page works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("A front-end example page for the project")
    }

    @Test
    fun `redirectTo returns a redirect when the key exists, is safe and reachable`() {
        val target = shortUrl("http://shop.mango.com/es").headers.location
        require(target != null)
        TimeUnit.SECONDS.sleep(2L)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://shop.mango.com/es"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a bad request with retry-after when the key exists and safety is unknown`() {
        val target = shortUrl("http://shop.mango.com/es").headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.headers.get("Retry-After")).isEqualTo(listOf("500"))
        assertThat(response.headers.location).isEqualTo(URI.create("http://shop.mango.com/es"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `redirectTo returns a forbidden when the key exists and is not safety`() {
        val target = shortUrl("https://testsafebrowsing.appspot.com/s/malware.html").headers.location
        require(target != null)
        TimeUnit.SECONDS.sleep(2L)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `redirectTo returns a bad request when the key exists and is not reachable`() {
        val target = shortUrl("http://example.com/health").headers.location
        require(target != null)
        TimeUnit.SECONDS.sleep(2L)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val response = restTemplate.getForEntity("http://localhost:$port/f684a3c4", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val response = shortUrl("http://shop.mango.com/es")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/f9e0870d"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/f9e0870d"))
        assertThat(response.body?.properties?.get("qr")).isNull()

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash with qr`() {
        val response = shortUrlQR("http://shop.mango.com/es")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/f9e0870d"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/f9e0870d"))
        assertThat(response.body?.properties?.get("qr")).isEqualTo("http://localhost:$port/f9e0870d/qr")

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        val response = shortUrl("ftp://shop.mango.com/es")

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(0)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `qr returns an image when the key exists`() {
        shortUrlQR("http://shop.mango.com/es")
        TimeUnit.SECONDS.sleep(2L)
        val response = callQR("http://localhost:$port/f9e0870d/qr")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
    }

    @Test
    fun `qr returns a not found when the key does not exist`() {

        val response = callQR("http://localhost:$port/f684a3c4/qr")
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    private fun shortUrl(url: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url
        data["qr"] = "false"

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java
        )
    }

    private fun shortUrlQR(url: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url
        data["qr"] = "true"

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java
        )
    }

    private fun callQR(url: String): ResponseEntity<ByteArrayResource> {
        return restTemplate.getForEntity(url, HttpHeaders(), ByteArrayResource::class.java)
    }
}
