package me.kpavlov.mocks.statsd.client

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * This simple client supports three metric types:
 * counters, timers and gauges. Each function prepares a metric string
 * according to the StatsD protocol and sends it
 * to the StatsD server using a UDP socket.
 *
 * @link https://sysdig.com/blog/monitoring-statsd-metrics/
 */
public class StatsDClient(
    private val host: String = "localhost",
    private val port: Int = 8125
) {
    private val socket: DatagramSocket = DatagramSocket()

    @JvmOverloads
    public fun incrementCounter(metric: String, delta: Long = 1, sampleRate: Double? = null) {
        if (sampleRate != null) {
            send("$metric:$delta|c|@$sampleRate")
        } else {
            send("$metric:$delta|c")
        }
    }

    @JvmOverloads
    public fun decrementCounter(metric: String, sampleRate: Double? = null) {
        incrementCounter(metric, -1, sampleRate)
    }

    @JvmOverloads
    public fun time(metric: String, value: Long, sampleRate: Double? = null) {
        if (sampleRate != null) {
            send("$metric:$value|ms|@$sampleRate")
        } else {
            send("$metric:$value|ms")
        }
    }

    public fun gauge(metric: String, value: Double) {
        send("$metric:$value|g")
    }

    public fun histogram(metric: String, value: Double) {
        send("$metric:$value|h")
    }

    public fun meter(metric: String, value: Double) {
        send("$metric:$value|m")
    }

    public fun set(metric: String, value: Double) {
        send("$metric:$value|s")
    }

    /**
     * Send raw data (string) to server
     */
    public fun send(data: String) {
        println("Sending data: \"$data\"")
        val buffer = data.toByteArray()
        val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(host), port)
        socket.send(packet)
    }
}
