package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.usecases.ClickSum
import es.unizar.urlshortener.core.usecases.ClickUserSum
import org.springframework.http.HttpStatus

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    override fun computeClickSum(): List<ClickSum> = clickEntityRepository.computeClickSum()
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {

    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    override fun computeUserClicks(): List<ClickUserSum> = shortUrlEntityRepository.computeUserClicks()

    /**
     * Change the security of the url associate with id to true
     */
    override fun updateSecuritySecure(id: String) {
        val su = findByKey(id)
        su?.properties?.safe = true

        if (su != null) {
            shortUrlEntityRepository.save(su.toEntity())
        }
    }

    /**
     * Change the security and the mode of the url associate with id to false and 403
     */
    override fun changeSecurityGoogle(id: String) {
        val su = findByKey(id)
        su?.properties?.safe = false
        su?.redirection?.mode = HttpStatus.FORBIDDEN.value()

        if (su != null) {
            shortUrlEntityRepository.save(su.toEntity())
        }
    }
}
