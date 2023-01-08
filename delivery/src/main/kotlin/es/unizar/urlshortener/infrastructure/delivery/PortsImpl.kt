package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.QrService
import es.unizar.urlshortener.core.ValidatorService
import io.github.g0dkar.qrcode.QRCode
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val N_GOOGLE_GOOD_RESPONSE = 3

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    @Autowired
    lateinit var rabbitTemplate: RabbitTemplate
    override fun isValid(url: String) = urlValidator.isValid(url)

    /**
     *   Send the message to Rabbitmq
     */
    override fun sendToRabbit(url: String, id: String) {

        rabbitTemplate.convertAndSend(
            "urlshort",
            "$url $id"
        )
    }

    /**
     * Implementation of the functionality of Google Safe Browsing
     * Return true if the response is empty *(" {}")* that means that the url is secure, false otherwise
     */
    override fun googleSafeBrowsing(url: String): Boolean {
        val keyURI = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" +
            "AIzaSyA-X7PVj9e2vuaxT5nqyp58TcFa4gBQNo4"
        val valuesGoogle = """
        {
            "client":{
                "clientId":"accessSafe",
                "clientVersion":"1.0"
            },
            "threatInfo":{
                "threatTypes":["MALWARE","SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "THREAT_TYPE_UNSPECIFIED",
                    "POTENTIALLY_HARMFUL_APPLICATION"], 
                "platformTypes":["ANY_PLATFORM"],
                "threatEntryTypes":["URL"],
                "threatEntries":[{"url":"%s"}]
            }
        }
    """

        // Código inspirado en https://zetcode.com/kotlin/getpostrequest/
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(keyURI))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(valuesGoogle.format(url)))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.toString().contains("200") && response.body().length <= N_GOOGLE_GOOD_RESPONSE
    }

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
@Suppress("UnstableApiUsage")
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}

/**
 * Implementation of the port [QrService].
 */
class QrServiceImpl : QrService {
    override fun getQr(url: String): ByteArray =
        ByteArrayOutputStream().let {
            QRCode(url).render().writeImage(it)
            it.toByteArray()
        }
}
