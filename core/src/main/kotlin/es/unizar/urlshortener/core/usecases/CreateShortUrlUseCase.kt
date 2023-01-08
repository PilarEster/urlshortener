package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.UrlNotSafe
import es.unizar.urlshortener.core.ValidatorService

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties): ShortUrl =

        shortUrlRepository.findByKey(hashService.hasUrl(url))?.let { short ->
            short.properties.safe?.let {
                if (!it) {
                    throw UrlNotSafe(url)
                }
            }

            if (short.properties.qr == false && data.qr == true) {
                val shortUrl = ShortUrl(
                    hash = hashService.hasUrl(url),
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                        ip = data.ip,
                        sponsor = data.sponsor,
                        qr = data.qr,
                        safe = short.properties.safe
                    )
                )

                shortUrlRepository.save(shortUrl)
            } else {
                short
            }
        } ?: run {
            if (validatorService.isValid(url)) {
                val id: String = hashService.hasUrl(url)
                val short = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                        ip = data.ip,
                        sponsor = data.sponsor,
                        qr = data.qr
                    )
                )
                validatorService.sendToRabbit(url, id)

                shortUrlRepository.save(short)
            } else {
                throw InvalidUrlException(url)
            }
        }
}
