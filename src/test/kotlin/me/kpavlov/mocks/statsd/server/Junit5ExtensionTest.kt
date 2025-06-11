package me.kpavlov.mocks.statsd.server

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import me.kpavlov.mocks.statsd.client.StatsDClient
import me.kpavlov.mocks.statsd.junit5.StatsDJUnit5Extension
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(StatsDJUnit5Extension::class)
internal class Junit5ExtensionTest : BaseStatsDServerTest() {

    @BeforeAll
    fun beforeAll() {
        statsd = StatsDJUnit5Extension.statsDServer()
        client = StatsDClient(port = statsd.port())
    }

    @Test
    fun `Server should run`() {
        statsd.port() shouldBeGreaterThan 1
        statsd.host() shouldNotBeNull {}
    }
}
