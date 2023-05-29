package me.kpavlov.mocks.statsd.server

import me.kpavlov.mocks.statsd.client.StatsDClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test

@Suppress("UnnecessaryAbstractClass")
internal abstract class BaseStatsDServerTest {
    protected lateinit var mockStatsD: MockStatsDServer
    protected lateinit var client: StatsDClient

    @Test
    fun `Server should capture Time`() {
        val name = "timeMetric"
        val value = 31L
        client.time(name, value)
        await untilAsserted {
            assertThat(mockStatsD.metric(name)).isEqualTo(value.toDouble())
        }
        val expectedMessage = "$name:$value|ms"
        assertThat(mockStatsD.calls()).containsOnlyOnce(expectedMessage)
        mockStatsD.verifyCall(expectedMessage)
        mockStatsD.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should capture Counter`() {
        val name = "counterMetric"
        client.incrementCounter(name)
        client.incrementCounter(name)
        client.incrementCounter(name)
        await untilAsserted {
            assertThat(mockStatsD.metric(name)).isEqualTo(3.0)
        }
        val expectedMessage = "$name:1|c"
        assertThat(mockStatsD.calls().count { it == expectedMessage }).isEqualTo(3)
        mockStatsD.verifyCall(expectedMessage)
        mockStatsD.verifyCall(expectedMessage)
        mockStatsD.verifyCall(expectedMessage)
        mockStatsD.verifyNoMoreCalls(expectedMessage)
    }
    
    @Test
    fun `Should reset`() {
        val name = "counterMetric"
        client.incrementCounter(name)
        val expectedMessage = "$name:1|c"
        await untilAsserted {
            assertThat(mockStatsD.calls()).contains(expectedMessage)
        }
        // when
        mockStatsD.reset()
        assertThat(mockStatsD.calls()).isEmpty()
        mockStatsD.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should capture Gauge`() {
        val name = "gaugeMetric"
        val value = 42.0
        client.gauge(name, value)
        await untilAsserted {
            assertThat(mockStatsD.metric(name)).isEqualTo(value)
        }
        val expectedMessage = "$name:$value|g"
        assertThat(mockStatsD.calls()).containsOnlyOnce(expectedMessage)
        mockStatsD.verifyCall(expectedMessage)
        mockStatsD.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should capture Histogram`() {
        val name = "histogramMetric"
        val value = 42.0
        client.histogram(name, value)
        await untilAsserted {
            assertThat(mockStatsD.metric(name)).isEqualTo(value)
        }
        val expectedMessage = "$name:$value|h"
        assertThat(mockStatsD.calls()).containsOnlyOnce(expectedMessage)
        mockStatsD.verifyCall(expectedMessage)
        mockStatsD.verifyNoMoreCalls(expectedMessage)
    }
}
