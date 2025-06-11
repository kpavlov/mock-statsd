package me.kpavlov.mocks.statsd.server

import org.awaitility.kotlin.await
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.test.Test

internal class ServerShutdownTest {

    @Test
    fun `should start and stop server`() {
        val server = StatsDServer(port = RANDOM_PORT)

        server.start()

        val allocatedPort = server.port()
        val host = server.host()

        await.until {
            !isPortAvailable(host, allocatedPort)
        }

        server.stop()

        await.until {
            isPortAvailable(host, allocatedPort)
        }
    }

    private fun isPortAvailable(host: String, port: Int): Boolean {
        return try {
            // Try to create a ServerSocket on the port
            DatagramSocket(port, InetAddress.getByName(host)).use {
                // If successful, the port is available
                true
            }
        } catch (e: java.io.IOException) {
            // If an IOException is thrown, the port is likely in use
            false
        }
    }
}
