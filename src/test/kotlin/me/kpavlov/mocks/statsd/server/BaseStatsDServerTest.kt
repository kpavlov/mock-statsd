package me.kpavlov.mocks.statsd.server

import me.kpavlov.mocks.statsd.client.StatsDClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test

/**
 * See also: https://github.com/statsd/statsd/blob/master/docs/metric_types.md
 */
@Suppress("UnnecessaryAbstractClass")
internal abstract class BaseStatsDServerTest {
    protected lateinit var statsd: MockStatsDServer
    protected lateinit var client: StatsDClient

    @Test
    fun `Server should capture Timer`() {
        val name = "timeMetric"
        val value = 31L
        client.time(name, value)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(value.toDouble())
        }
        val expectedMessage = "$name:$value|ms"
        assertThat(statsd.calls()).containsOnlyOnce(expectedMessage)
        statsd.verifyCall(expectedMessage)
        statsd.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should capture Timer with sample rate`() {
        val name = "sampleTimeMetric"
        val value = 31L
        client.time(name, value, 0.1)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(value.toDouble())
        }
        val expectedMessage = "$name:$value|ms|@0.1"
        assertThat(statsd.calls()).containsOnlyOnce(expectedMessage)
        statsd.verifyCall(expectedMessage)
        statsd.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should capture Counter`() {
        val name = "counterMetric"
        client.incrementCounter(name)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(1.0)
        }
        client.incrementCounter(name, 5)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(6.0)
        }
        client.incrementCounter(name, -2)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(4.0)
        }
        client.decrementCounter(name)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(3.0)
        }
        statsd.verifyCall("$name:1|c")
        statsd.verifyCall("$name:5|c")
        statsd.verifyCall("$name:-2|c")
        statsd.verifyCall("$name:-1|c")
    }

    @Test
    fun `Should reset`() {
        val name = "counterMetric"
        client.incrementCounter(name)
        val expectedMessage = "$name:1|c"
        await untilAsserted {
            assertThat(statsd.calls()).contains(expectedMessage)
        }
        // when
        statsd.reset()
        assertThat(statsd.calls()).isEmpty()
        statsd.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should capture Gauge`() {
        val name = "gaugeMetric"
        val value = 42.0
        client.gauge(name, value)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(value)
        }
        val expectedMessage = "$name:$value|g"
        assertThat(statsd.calls()).containsOnlyOnce(expectedMessage)
        statsd.verifyCall(expectedMessage)
        statsd.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should capture Set metric`() {
        val name = "setMetric"
        val values = listOf(42.0, 42.0, 43.2, 44.5)

        values.forEach {
            client.set(name, it)
            await untilAsserted {
                assertThat(statsd.metricContents(name)).contains(it)
            }
        }

        assertThat(statsd.metricContents(name))
            .containsExactlyElementsOf(values.toSet())
    }

    /**
     * This test case ensures that gauge metrics in StatsD work correctly,
     * including setting an initial value and adjusting it.
     * It also verifies that the server processes the operations
     * in the correct order and that there are no additional,
     * unexpected calls to the server.
     */
    @Test
    fun `Server should capture and adjust Gauge`() {
        val name = "gaugor"

        // Set initial value
        var value = 333.0
        client.gauge(name, value)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(value)
        }

        // Decrease the value
        value -= 10
        client.gauge(name, -10.0)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(value)
        }
        statsd.verifyCall("$name:-10.0|g")

        // Increase the value
        value += 4
        client.gauge(name, 4.0)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(value)
        }
        statsd.verifyCall("$name:4.0|g")
    }

    @Test
    fun `Server should capture Histogram`() {
        val name = "histogramMetric"
        val value = 42.0
        client.histogram(name, value)
        await untilAsserted {
            assertThat(statsd.metric(name)).isEqualTo(value)
        }
        val expectedMessage = "$name:$value|h"
        assertThat(statsd.calls()).containsOnlyOnce(expectedMessage)
        statsd.verifyCall(expectedMessage)
        statsd.verifyNoMoreCalls(expectedMessage)
    }

    @Test
    fun `Server should handle multi-metric packets`() {
        val gaugeName = "batchGauge"
        val counterName = "batchCounter"

        // Set initial value
        val gaugeValue = 333.0
        val counterValue = 42.0
        client.send("$gaugeName:$gaugeValue|g\n$counterName:$counterValue|c")
        await untilAsserted {
            assertThat(statsd.metric(gaugeName)).isEqualTo(gaugeValue)
        }
        await untilAsserted {
            assertThat(statsd.metric(counterName)).isEqualTo(counterValue)
        }
    }
}
