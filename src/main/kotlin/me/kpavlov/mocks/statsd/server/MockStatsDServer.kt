package me.kpavlov.mocks.statsd.server

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A StatsD server that listens for incoming UDP packets containing metrics and stores them for analysis.
 * The server runs on a specified port or the default port (8125) if not provided.
 */
public class MockStatsDServer(
    host: String = DEFAULT_HOST,
    port: Int = DEFAULT_PORT
) : StatsDServer(host = host, port = port) {
    private val calls = ConcurrentLinkedQueue<String>()

    protected override fun onMessage(message: String) {
        calls.add(message)
    }

    /**
     * Retrieves the list of calls received by the server.
     *
     * @return the list of calls
     */
    public fun calls(): List<String> = calls.toList()

    /**
     * Reset collected metrics and recorded calls.
     */
    override fun reset() {
        calls.clear()
        super.reset()
    }

    /**
     * Verifies that the specified message was received by the server.
     * Throws an AssertionError if the message is not found in the received messages.
     *
     * @param message the message to verify
     * @throws AssertionError if the message is not found
     */
    @Throws(AssertionError::class)
    public fun verifyCall(message: String) {
        if (calls.remove(message)) {
            // ok, message was found and removed
        } else {
            throw AssertionError("No message found: $message")
        }
    }

    /**
     * Verifies that no more calls with the specified message are received by the server.
     * Throws an AssertionError if the message is found in the received messages.
     *
     * @param message the message to verify
     * @throws AssertionError if the message is found
     */
    @Throws(AssertionError::class)
    public fun verifyNoMoreCalls(message: String) {
        if (calls.contains(message)) {
            throw AssertionError("Unexpected message received: $message")
        } else {
            // ok, message not found
        }
    }
}
