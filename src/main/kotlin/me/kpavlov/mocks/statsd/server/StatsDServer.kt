package me.kpavlov.mocks.statsd.server

import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

public const val RANDOM_PORT: Int = 0
public const val DEFAULT_PORT: Int = 8125

private const val BUFFER_SIZE = 1024

/**
 * A StatsD server that listens for incoming UDP packets containing metrics and stores them for analysis.
 * The server runs on a specified port or the default port (8125) if not provided.
 */
public open class StatsDServer(port: Int = DEFAULT_PORT) {
    private val logger = LoggerFactory.getLogger(StatsDServer::class.java)
    private val socket = DatagramSocket(port)
    private val metrics = ConcurrentHashMap<String, Double>()
    private val executorService = Executors.newSingleThreadExecutor()

    private var shouldRun = false

    /**
     * Retrieves the port number on which the server is running.
     *
     * @return the port number
     */
    public fun port(): Int = socket.localPort

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
                onMessage(message)
                val metricData = message.split(":")
                val metricName = metricData[0]
                val metricValue = metricData[1].split("|")[0].toDouble()
                metrics.merge(metricName, metricValue, Double::plus)
                logger.debug("Updated value: {} = {}", metricName, metrics[metricName])
            }
        }
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

    /**
     * Retrieves the metric value for the specified metric name.
     *
     * @param metricName the name of the metric
     * @return the metric value associated with the metric name, or `null` if not found
     */
    public fun metric(metricName: String): Double? {
        return metrics[metricName]
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
