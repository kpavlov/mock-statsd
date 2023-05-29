package me.kpavlov.mocks.statsd.server

import me.kpavlov.mocks.statsd.client.StatsDClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StatsDServerTest : BaseStatsDServerTest() {

    @BeforeAll
    fun beforeAll() {
        statsd = MockStatsDServer(RANDOM_PORT)
        statsd.start()
        client = StatsDClient(port = statsd.port())
    }

    @AfterAll
    fun afterAll() {
        statsd.stop()
    }
}
