package es.unizar.urlshortener.core.concurrent

import es.unizar.urlshortener.core.usecases.QrCodeUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue

/**
 * Concurrent process that generates QR Codes following a queue order.
 *
 * To manage the concurrence, it's being using a [BlockingQueue].
 */
@Component
open class QRBlockingQueue(
    private val qrQueue: BlockingQueue<Pair<String, String>>,
    private val qrCodeUseCase: QrCodeUseCase
) {
    @Async("concurrentConfig")
    @Scheduled(fixedDelay = 500L)
    open
    fun executor() {
        if (!qrQueue.isEmpty()) {
            val result = qrQueue.take()
            qrCodeUseCase.generateQR(result.first, result.second)
        }
    }
}
