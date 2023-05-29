# Mock StatsD

[![Maven Central](https://img.shields.io/maven-central/v/com.github.kpavlov.mocks.statsd/mock-statsd)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A"com.github.kpavlov.mocks.statsd"%20AND%20a%3A"mock-statsd")
[![Java CI with Gradle](https://github.com/kpavlov/mock-statsd/actions/workflows/gradle.yml/badge.svg)](https://github.com/kpavlov/mock-statsd/actions/workflows/gradle.yml)
[![CodeQL](https://github.com/kpavlov/mock-statsd/actions/workflows/codeql.yml/badge.svg)](https://github.com/kpavlov/mock-statsd/actions/workflows/codeql.yml)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-1.8.21-blue.svg?logo=kotlin)](http://kotlinlang.org)

**TL;DR** The Mock StatsD library simplifies the testing process for Kotlin/Java applications that send metrics to a StatsD server. It provides detailed verification capabilities for all types of StatsD metrics and automates port management for ease of use. It integrates smoothly with JUnit 5 for effortless unit testing and leverages Kotlin features for an idiomatic and intuitive experience. Ultimately, Mock StatsD promotes software quality and reliability by enabling thorough and efficient metrics testing.

## What is StatsD?

[StatsD](https://github.com/statsd/statsd) is a simple daemon for easy stats aggregation which is commonly used for monitoring applications. The basic idea is to send different types of metrics (like counters, timers, gauges) from your application to StatsD, which then periodically aggregates the metrics and pushes them to Graphite (or some other defined backend).

# Mock StatsD Server library in Kotlin

The **Mock StatsD** library offers several benefits for Kotlin and Java developers, especially those working on applications that send metrics to a StatsD server:

1. **Simplified Testing**: By providing a mock StatsD server, this tool makes it easier to write unit tests for your application's metrics-related functionality. Instead of having to manage a real StatsD server for testing purposes, you can simply start and stop the mock server as needed.

2. **Detailed Verification**: Mock StatsD allows you to verify the exact metrics sent by your application, including the type, name, and value of each metric. This makes it possible to catch subtle bugs in your metrics code.

3. **Support for All StatsD Metric Types**: Whether your application uses counters, timers, gauges, or histograms, you can test them all with Mock StatsD.

4. **Automatic Port Management**: If you don't specify a port number when creating the mock server, it automatically selects an available port. This is especially useful in environments where many tests are running concurrently and may be using network resources.

5. **Easy Integration with JUnit 5**: The library provides a JUnit 5 extension, which automatically manages the lifecycle of the mock server. This means the server will start before your tests run and stop after they complete, minimizing the boilerplate code in your tests.

6. **Built with Kotlin**: As a Kotlin library, Mock StatsD integrates seamlessly with your Kotlin codebase. It takes advantage of Kotlin features like extension functions, and its API is designed to be idiomatic and intuitive for Kotlin developers.

By simplifying the process of testing your application's metrics code and providing detailed, easy-to-use verification methods, Mock StatsD helps ensure the quality and reliability of your software.

## Getting Started

1. Add dependency in `pom.xml`:
    ```xml
    <dependency>
        <groupId>com.github.kpavlov.mocks.statsd</groupId>
        <artifactId>mock-statsd</artifactId>
        <version>${mock-statsd.version}</version>
        <scope>test</scope>
    </dependency>
    ```

    or in `build.gradle.kts`:
    ```kotlin
    dependencies {
        testImplementation("com.github.kpavlov.mocks.statsd:mock-statsd:$mockStatsdVersion")
    }
    ```

    Check latest version in [Maven Central repository](https://central.sonatype.com/artifact/com.github.kpavlov.mocks.statsd/mock-statsd)

2. To use this library, add the following import statement to your Kotlin file:

    ```kotlin
    import me.kpavlov.mocks.statsd.server.MockStatsDServer
    ```

3. Create a new instance of [`MockStatsDServer`](src/main/kotlin/me/kpavlov/mocks/statsd/MockStatsDServer.kt) with, specifying the port number. Use `RANDOM_PORT` to automatically select an available port:

    ```kotlin
    val mockStatsD = MockStatsDServer(RANDOM_PORT)
    ```

4. Start the server:

    ```kotlin
    mockStatsD.start()
    ```

    You can now send metrics to the server and then verify that they were received correctly.

5. Cleaning Up

    ```kotlin
    mockStatsD.reset()
    ```

    Cleans up collected metrics and recorded calls.

6. Stopping server

    When you're done with the server, stop it with the `stop` method:

    ```kotlin
    mockStatsD.stop()
    ```

    This ensures that the port used by the server is freed up and can be used by other processes.

### JUnit5 Extension

You can also register MockStatsDServer as JUnit 5 [extension](https://junit.org/junit5/docs/current/user-guide/#extensions).
It will automatically create and start a single instance of `MockStatsDServer` to use in all tests.
It will be stopped on JVM shutdown, when test execution is finished.

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(StatsDJUnit5Extension::class)
internal class Junit5ExtensionTest : BaseStatsDServerTest() {

    lateinit var mockStatsD: MockStatsDServer
    lateinit var client: StatsDClient

    @BeforeAll
    fun beforeAll() {
        mockStatsD = StatsDJUnit5Extension.statsDServer()
        client = StatsDClient(port = mockStatsD.port())
    }
}
```
Now you can use `MockStatsD` server and statsD client in tests.

## Sending Metrics

The server can capture different types of metrics: Time, Counter, Gauge, and Histogram. Here's how to send each type of metric:

### Time

```kotlin
val name = "timeMetric"
val value = 31L
client.time(name, value)
```

### Counter

```kotlin
val name = "counterMetric"
client.incrementCounter(name)
```

### Gauge

```kotlin
val name = "gaugeMetric"
val value = 42.0
client.gauge(name, value)
```

### Histogram:

```kotlin
val name = "histogramMetric"
val value = 42.0
client.histogram(name, value)
```

### Multi-Metric Packets

MockStatsD supports receiving  [multiple metrics in a single packet](https://github.com/statsd/statsd/blob/master/docs/metric_types.md#multi-metric-packets) by separating them with a newline characters (`\n`).

    batchGauge:333.0|g\nbatchCounter:42.0|c

Each message could therefore represent a batch of metrics sent from the client in a single UDP packet. This is useful for reducing network load when sending multiple metrics, as it can all be done in one network operation.

The batch support is achieved by first splitting the batched message into individual metrics and then handling each metric separately, e.g.:

```kotlin
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

```

## Verifying Metrics

After sending metrics, you can verify that the server captured them correctly. Use the `metric` method to retrieve a metric value, and `verifyCall` to verify that a specific call was made. For example:

```kotlin
await untilAsserted {
    assertThat(mockStatsD.metric(name)).isEqualTo(value.toDouble())
}
println(mockStatsD.calls()) // prints all calls to console
mockStatsD.verifyCall("$name:$value|ms")
mockStatsD.verifyNoMoreCalls("$name:$value|ms")
```

This example checks that the server received a Time metric with the specified name and value, and that no more calls with the same name and value were made.

## Complete Example

Check out the [`StatsDServerTest.kt`](src/test/kotlin/me/kpavlov/mocks/statsd/server/StatsDServerTest.kt) file in the `test` directory for a complete example of how to use the `StatsDServer`. This test class demonstrates how to set up a server, send different types of metrics, verify the captured metrics, and clean up the server.
