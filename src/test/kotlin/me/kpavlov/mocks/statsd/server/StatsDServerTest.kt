package me.kpavlov.mocks.statsd.server

import me.kpavlov.mocks.statsd.client.StatsDClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StatsDServerTest {

    private lateinit var server: StatsDServer
    private lateinit var client: StatsDClient

    @BeforeAll
    fun beforeAll() {
        server = StatsDServer(RANDOM_PORT)
        server.start()
        client = StatsDClient(port = server.port())
    }

    @AfterAll
    fun afterAll() {
        server.stop()
    }

    @Test
    fun `Server should capture Time`() {
        val name = "timeMetric"
        val value = 31L
        client.time(name, value)
        await untilAsserted {
            assertThat(server.metric(name)).isEqualTo(value.toDouble())
        }
        server.verifyCall("$name:$value|ms")
        server.verifyNoMoreCalls("$name:$value|ms")
    }

    @Test
    fun `Server should capture Counter`() {
        val name = "counterMetric"
        client.incrementCounter(name)
        client.incrementCounter(name)
        client.incrementCounter(name)
        await untilAsserted {
            assertThat(server.metric(name)).isEqualTo(3.0)
        }
        server.verifyCall("$name:1|c")
        server.verifyCall("$name:1|c")
        server.verifyCall("$name:1|c")
        server.verifyNoMoreCalls("$name:1|c")
    }

    @Test
    fun `Server should capture Gauge`() {
        val name = "gaugeMetric"
        val value = 42.0
        client.gauge(name, value)
        await untilAsserted {
            assertThat(server.metric(name)).isEqualTo(value)
        }
        server.verifyCall("$name:$value|g")
        server.verifyNoMoreCalls("$name:$value|g")
    }

    @Test
    fun `Server should capture Histogram`() {
        val name = "histogramMetric"
        val value = 42.0
        client.histogram(name, value)
        await untilAsserted {
            assertThat(server.metric(name)).isEqualTo(value)
        }
        server.verifyCall("$name:$value|h")
        server.verifyNoMoreCalls("$name:$value|h")
    }
}
