package me.kpavlov.mocks.statsd.junit5

import me.kpavlov.mocks.statsd.server.MockStatsDServer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext

public class StatsDJUnit5Extension : Extension, BeforeAllCallback {

    public companion object {
        private var statsd: MockStatsDServer? = null

        public fun statsDServer(): MockStatsDServer = statsd!!
    }

    override fun beforeAll(context: ExtensionContext) {
        if (statsd != null) {
            return
        }
        statsd = MockStatsDServer()
        statsd?.start()

        Runtime.getRuntime().addShutdownHook(
            Thread {
                statsd?.stop()
            }
        )
    }
}
