package me.kpavlov.mocks.statsd.server

import me.kpavlov.mocks.statsd.client.StatsDClient
import me.kpavlov.mocks.statsd.junit5.StatsDJUnit5Extension
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(StatsDJUnit5Extension::class)
internal class Junit5ExtensionTest : BaseStatsDServerTest() {

    @BeforeAll
    fun beforeAll() {
        statsd = StatsDJUnit5Extension.statsDServer()
        client = StatsDClient(port = statsd.port())
    }

    @BeforeEach
    fun beforeEach() {
        client = StatsDClient(port = statsd.port())
    }

    @AfterAll
    fun afterAll() {
        statsd.stop()
    }
}
