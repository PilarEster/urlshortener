package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.InfoNotAvailable
import es.unizar.urlshortener.core.QrService
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Class that manages QR Codes generation.
 *
 * *[generateQR]* generates a QR Code using an id and an url.
 *
 * *[getQR]* provides a QR Code using an id.
 *
 * **Note**: To provide a QR it must be created previously.
 */
interface QrCodeUseCase {
    fun generateQR(id: String, url: String)

    fun getQR(id: String): ByteArray
}

/**
 * Implementation of [QrCodeUseCase].
 */
class QrCodeUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val qrService: QrService,
    private val qrMap: HashMap<String, ByteArray>
) : QrCodeUseCase {

    override fun generateQR(id: String, url: String) {
        shortUrlRepository.findByKey(id)?.let {
            qrMap.put(id, qrService.getQr(url))
        } ?: throw RedirectionNotFound(id)
    }

    override fun getQR(id: String): ByteArray =
        shortUrlRepository.findByKey(id)?.let {
            if (it.properties.qr == true) {
                qrMap.get(id)
            } else {
                throw InfoNotAvailable(id, "QR")
            }
        } ?: throw RedirectionNotFound(id)
}
