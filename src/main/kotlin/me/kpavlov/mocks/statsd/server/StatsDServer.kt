package me.kpavlov.mocks.statsd.server

import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

public const val RANDOM_PORT: Int = 0
public const val DEFAULT_PORT: Int = 8125
public const val DEFAULT_HOST: String = "127.0.0.1"

private const val BUFFER_SIZE = 8932

/**
 * A StatsD server that listens for incoming UDP packets containing metrics and stores them for analysis.
 * The server runs on a specified port or the default port (8125) if not provided.
 */
@Suppress("TooManyFunctions")
public open class StatsDServer(
    initialHost: String = DEFAULT_HOST,
    private val initialPort: Int = DEFAULT_PORT
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val repository = MetricRepository()
    private val executorService = Executors.newCachedThreadPool()

    private var shouldRun = false

    private val host = InetAddress.getByName(initialHost)
    private var serverSocket: DatagramSocket? = null
    /**
     * Retrieves the port number on which the server is running.
     *
     * @return the port number
     */
    public fun port(): Int = serverSocket?.localPort ?: throw  IllegalStateException("Server is not started")
    public fun host(): String = serverSocket?.localAddress?.hostAddress ?: throw  IllegalStateException("Server is not started")

    /**
     * Reset collected metrics
     */
    public open fun reset(): Unit = repository.reset()

    /**
     * Starts the StatsD server, listening for incoming UDP packets and processing them.
     * This method runs on a separate thread.
     */
    public fun start() {
        val buffer = ByteArray(BUFFER_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)

        val latch = CountDownLatch(1)

        executorService.submit {
            shouldRun = true
            serverSocket = DatagramSocket(initialPort, host)
            logger.info("Starting StatsD server on ${host()}:${port()}")
            serverSocket.use { socket ->
                latch.countDown()
                while (shouldRun) {
                    socket?.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    if (logger.isDebugEnabled) {
                        logger.debug("Received: {}", message)
                    }
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
        latch.await()
    }

    private fun handleMessage(message: String) {
        message
            .split("\n")
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .forEach(this::handleMetric)
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
            val expression = valueParts[i]
            when (expression.first()) {
                '@' -> {
                    sampleRate = expression.substring(1).toDouble()
                }

                '#' -> {
                    tags = extractTags(expression)
                }
            }
        }

        repository.merge(metricType, metricName, tags, metricValue, sampleRate)
    }

    private fun extractTags(expression: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        expression.substring(1)
            .split(",")
            .forEach {
                val keysAndValues = it.split(':')
                for (t in 0 until keysAndValues.size - 1) {
                    val tagName = keysAndValues[t]
                    val tagValue = keysAndValues[t + 1]
                    result[tagName] = tagValue
                }
            }
        return result
    }

    protected open fun onMessage(message: String) {
        // do nothing here
    }

    /**
     * Stops the StatsD server, ceasing the packet processing and shutting down the server.
     */
    public fun stop() {
        logger.info("Stopping StatsD server on ${host()}:${port()}")
        shouldRun = false
        serverSocket?.close()
    }

    override fun close() {
        stop()
        executorService.shutdownNow()
    }

    private fun findMetric(metricName: String, tags: Map<String, String>? = null): Metric? {
        return repository.findMetric(metricName, tags)
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
    ): DoubleArray? = repository.metricContents(metricName, tags)
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
