# Mock StatsD

[![Maven Central](https://img.shields.io/maven-central/v/com.github.kpavlov.mocks.statsd/mock-statsd)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A"com.github.kpavlov.mocks.statsd"%20AND%20a%3A"mock-statsd")
[![Java CI with Gradle](https://github.com/kpavlov/mock-statsd/actions/workflows/gradle.yml/badge.svg)](https://github.com/kpavlov/mock-statsd/actions/workflows/gradle.yml)
[![CodeQL](https://github.com/kpavlov/mock-statsd/actions/workflows/codeql.yml/badge.svg)](https://github.com/kpavlov/mock-statsd/actions/workflows/codeql.yml)

## What is StatsD?

StatsD is a simple daemon for easy stats aggregation which is commonly used for monitoring applications. The basic idea is to send different types of metrics (like counters, timers, gauges) from your application to StatsD, which then periodically aggregates the metrics and pushes them to Graphite (or some other defined backend).

# StatsD Mock Server in Kotlin

This is a simple library for creating a mock StatsD server in Kotlin. It can be used for unit testing applications that send metrics to a StatsD server.

## Getting Started

1. Add dependency:

    pom.xml
    ```xml
    <dependency>
        <groupId>com.github.kpavlov.mocks.statsd</groupId>
        <artifactId>mock-statsd</artifactId>
        <version>${VERSION}</version>
        <scope>test</scope>
    </dependency>
    ```

   in `pom.xml`:
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
