package es.unizar.urlshortener.core

import es.unizar.urlshortener.core.usecases.ClickSum
import es.unizar.urlshortener.core.usecases.ClickUserSum

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
    fun computeClickSum(): List<ClickSum>
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
    fun computeUserClicks(): List<ClickUserSum>
    fun updateSecuritySecure(id: String)
    fun changeSecurityGoogle(id: String)
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean

    fun sendToRabbit(url: String, id: String)
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}

interface QrService {
    fun getQr(url: String): ByteArray
}
