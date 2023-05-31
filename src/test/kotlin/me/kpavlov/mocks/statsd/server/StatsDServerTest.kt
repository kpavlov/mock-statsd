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

    @Test
    fun `Should handle batch with empty lines`() {
        val packet = " \n \nlogback.events:1|c|#statistic:count,level:info \n \n"

        client.send(packet)

        await untilAsserted {
            assertThat(
                statsd.metric(
                    metricName = "logback.events",
                    tags = mapOf(
                        "statistic" to "count",
                        "level" to "info"
                    )
                )
            )
                .isEqualTo(1.0)
        }
    }


    @Test
    fun `Should handle SpringBoot metrics batch`() {
        val packet = "logback.events:1|c|#statistic:count,level:info\n" +
            "logback.events:1|c|#statistic:count,level:info\n" +
            "http.server.requests:75.173334|ms|#error:none,exception:none,method:GET," +
            "outcome:SUCCESS,status:200,uri:/v2/user/{username}\n" +
            "http.server.requests:77|ms|#method:GET,status:200 OK,uri:/v2/user/jey\n" +
            "logback.events:1|c|#statistic:count,level:info\n" +
            "jvm.buffer.memory.used:46986|g|#statistic:value,id:direct\n" +
            "jvm.threads.states:0|g|#statistic:value,state:blocked\n" +
            "process.uptime:4141|g|#statistic:value\n" +
            "jvm.memory.used:10589472|g|#statistic:value,area:nonheap,id:Compressed Class Space\n" +
            "jvm.threads.states:17|g|#statistic:value,state:runnable\n" +
            "system.load.average.1m:4.591797|g|#statistic:value\n" +
            "jvm.memory.used:73195544|g|#statistic:value,area:nonheap,id:Metaspace\n" +
            "jvm.buffer.count:0|g|#statistic:value,id:mapped - 'non-volatile memory'\n" +
            "jvm.memory.committed:54525952|g|#statistic:value,area:heap,id:G1 Old Gen\n" +
            "r2dbc.pool.max.allocated:10|g|#statistic:value,name:connectionFactory\n" +
            "jvm.memory.used:4076672|g|#statistic:value,area:nonheap," +
            "id:CodeHeap 'non-profiled nmethods'\n" +
            "executor.queue.remaining:2147483647|g|#statistic:value," +
            "name:applicationTaskExecutor\n" +
            "jvm.buffer.total.capacity:46985|g|#statistic:value,id:direct\n" +
            "jvm.memory.committed:88080384|g|#statistic:value,area:heap,id:G1 Eden Space\n" +
            "executor.pool.size:0|g|#statistic:value,name:applicationTaskExecutor"

        client.send(packet)

        await untilAsserted {
            assertThat(
                statsd.metric(
                    metricName = "jvm.memory.committed",
                    tags = mapOf(
                        "id" to "G1 Eden Space",
                        "statistic" to "value",
                        "area" to "heap"
                    )
                )
            )
                .isEqualTo(88080384.0)
        }
        await untilAsserted {
            assertThat(
                statsd.metric(
                    metricName = "http.server.requests",
                    tags = mapOf("uri" to "/v2/user/jey")
                )
            ).isEqualTo(77.0)
        }
    }
}
