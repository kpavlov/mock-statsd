package me.kpavlov.mocks.statsd.server

import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

public const val RANDOM_PORT: Int = 0
public const val DEFAULT_PORT: Int = 8125

private const val BUFFER_SIZE = 8932

/**
 * A StatsD server that listens for incoming UDP packets containing metrics and stores them for analysis.
 * The server runs on a specified port or the default port (8125) if not provided.
 */
public open class StatsDServer(port: Int = DEFAULT_PORT) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val socket = DatagramSocket(port)
    private val metrics = ConcurrentHashMap<MetricId, Metric>()
    private val executorService = Executors.newSingleThreadExecutor()

    private var shouldRun = false

    /**
     * Retrieves the port number on which the server is running.
     *
     * @return the port number
     */
    public fun port(): Int = socket.localPort

    /**
     * Reset collected metrics
     */
    public open fun reset() {
        metrics.clear()
    }

    /**
     * Starts the StatsD server, listening for incoming UDP packets and processing them.
     * This method runs on a separate thread.
     */
    public fun start() {
        val buffer = ByteArray(BUFFER_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)

        executorService.submit {
            shouldRun = true
            logger.info("Starting StatsD server on port ${port()}")
            while (shouldRun) {
                socket.receive(packet)
                val message = String(packet.data, 0, packet.length)
                logger.debug("Received: {}", message)
                @Suppress("TooGenericExceptionCaught")
                try {
                    onMessage(message)
                    handleMessage(message)
                } catch (e: Exception) {
                    logger.error("Can't handle message: $message", e)
                }
            }
        }
    }

    private fun handleMessage(message: String) {
        message.split("\n").forEach(this::handleMetric)
    }

    /**
     * Split metric:
     * ```
     * <metric name>:<value>|c[|@<sample rate>]
     * ```
     */
    private fun handleMetric(message: String) {
        val metricData = message.split(":", limit = 2)
        val metricName = metricData[0]
        val valueParts = metricData[1].split("|")
        val metricValue = valueParts[0].toDouble()
        val metricType = valueParts[1]

        var sampleRate: Double? = null
        var tags: Map<String, String> = emptyMap()

        for (i in 2 until valueParts.size) {
            val prefix = valueParts[i].first()
            when (prefix) {
                '@' -> {
                    sampleRate = valueParts[i].substring(1).toDouble()
                }

                '#' -> {
                    tags = mutableMapOf()
                    valueParts[i].substring(1)
                        .split(",")
                        .forEach {
                            it.split(':')
                                .zipWithNext { k, v -> tags[k] = v }
                        }
                }
            }
        }

        val metricId = MetricId(metricName, tags)
        logger.trace("Tags: {}, ID={}", tags, metricId)

        val metric = metrics.computeIfAbsent(metricId) { createMetric(metricType) }
        metric.merge(metricValue, sampleRate)
        logger.debug("Updated value: {} = {}", metricId, metrics[metricId])
    }

    protected open fun onMessage(message: String) {
        // do nothing here
    }

    /**
     * Stops the StatsD server, ceasing the packet processing and shutting down the server.
     */
    public fun stop() {
        logger.info("Stopping StatsD server on port ${port()}")
        shouldRun = false
    }

    private fun findMetric(metricName: String, tags: Map<String, String>? = null): Metric? {
        return metrics.entries
            .firstOrNull { it.key.matches(metricName, tags) }?.value
    }

    /**
     * Retrieves the metric value for the specified metric name.
     *
     * @param metricName the name of the metric
     * @param tags optional tags for the metric
     * @return the metric value associated with the metric name,
     * or `null` if not found
     */
    @JvmOverloads
    public fun metric(metricName: String, tags: Map<String, String>? = null): Double? {
        return findMetric(metricName, tags)?.value()?.toDouble()
    }

    /**
     * Retrieves the Set metric values for the specified metric name.
     *
     * @param metricName the name of the metric
     * @param tags optional tags for the metric
     * @return the metric value associated with the metric name,
     * or `null` if not found
     */
    @JvmOverloads
    public fun metricContents(
        metricName: String,
        tags: Map<String, String>? = null
    ): Array<Double>? {
        val metric = findMetric(metricName, tags)
        return if (metric is Metric.SetMetric) {
            metric.values()
        } else {
            null
        }
    }
}

/**
 * The main entry point for the StatsD server application.
 * It creates an instance of the StatsDServer class and starts the server.
 *
 * @param args the command-line arguments (unused)
 */
@Suppress("UNUSED_PARAMETER")
public fun main(vararg args: String) {
    val server = StatsDServer()
    server.start()
}
