package es.unizar.urlshortener.infrastructure.delivery

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PortsImplKtTest {
    @Autowired
    private lateinit var validatorService: ValidatorServiceImpl

    @BeforeEach
    fun setUp() {
        validatorService = ValidatorServiceImpl()
    }

    @Test
    fun `function googleSafeBrowsing return true if the URL is secure`() {
        val secure = validatorService.googleSafeBrowsing("https://example.com")
        assertTrue(secure)
    }

    @Test
    fun `function googleSafeBrowsing return false if the URL isn't secure`() {
        val secure = validatorService.googleSafeBrowsing("https://testsafebrowsing.appspot.com/s/phishing.html")
        assertFalse(secure)
    }
}
