package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.QrServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.RabbitmqImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.OffsetDateTime
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Suppress("TooManyFunctions")
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository
) {
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun qrService() = QrServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun rankingUseCase() =
            RankingUseCaseImpl(shortUrlRepositoryService(), clickRepositoryService())

    @Bean
    fun qrCodeUseCase() =
        QrCodeUseCaseImpl(shortUrlRepositoryService(), qrService(), qrMap())

    @Bean
    fun reachableWebUseCase() = ReachableWebUseCaseImpl(reachableMap(), reachableQueue())

    @Bean
    fun qrQueue(): BlockingQueue<Pair<String, String>> = LinkedBlockingQueue()

    @Bean
    fun reachableQueue(): BlockingQueue<String> = LinkedBlockingQueue()

    @Bean
    fun qrMap(): HashMap<String, ByteArray> = HashMap()

    @Bean
    fun reachableMap(): HashMap<String, Pair<Boolean, OffsetDateTime>> = HashMap()

    companion object {
        const val topicExchangeName: String = "rabbitmq-exchange"
        const val queueName: String = "urlshort"
    }

    @Bean
    fun queue(): Queue {
        return Queue(queueName, true)
    }

    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(topicExchangeName)
    }

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with("url.#")
    }

    @Bean
    fun container(connectionFactory: ConnectionFactory, listenerAdapter: MessageListenerAdapter):
        SimpleMessageListenerContainer {
        val container: SimpleMessageListenerContainer = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory
        container.setQueueNames(queueName)
        container.setMessageListener(listenerAdapter)
        return container
    }

    @Bean
    fun listenerAdapter(receiver: RabbitmqImpl): MessageListenerAdapter {
        return MessageListenerAdapter(receiver, "proveUrl")
    }
}
