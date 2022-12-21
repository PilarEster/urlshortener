package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.usecases.ClickSum
import es.unizar.urlshortener.core.usecases.ClickUserSum
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?

    @Query("SELECT ip AS ip, COUNT(ip) AS sum FROM ShortUrlEntity GROUP BY ip ORDER BY ip DESC")
    fun computeUserClicks(): List<ClickUserSum>
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long> {
    @Query("SELECT hash AS hash, COUNT(hash) AS sum FROM ClickEntity GROUP BY hash ORDER BY hash DESC")
    fun computeClickSum(): List<ClickSum>
}
