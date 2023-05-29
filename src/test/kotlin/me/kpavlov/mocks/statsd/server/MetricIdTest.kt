package me.kpavlov.mocks.statsd.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MetricIdTest {

    @Test
    fun `Name match`() {
        assertThat(MetricId(name = "name").matches(wantedName = "name"))
            .isTrue()
    }

    @Test
    fun `Name do not match`() {
        assertThat(MetricId(name = "name").matches(wantedName = "another"))
            .isFalse()
    }

    @Test
    fun `Missing tags do not match`() {
        assertThat(
            MetricId(name = "name")
                .matches(wantedName = "name", wantedTags = mapOf("a" to "b"))
        ).isFalse()
    }

    @Test
    fun `Same tags do match`() {
        assertThat(
            MetricId(name = "name", tags = mapOf("a" to "b"))
                .matches(wantedName = "name", wantedTags = mapOf("a" to "b"))
        ).isTrue()
    }

    @Test
    fun `Less tags do match`() {
        assertThat(
            MetricId(name = "name", tags = mapOf("a" to "b", "c" to "d"))
                .matches(wantedName = "name", wantedTags = mapOf("a" to "b"))
        ).isTrue()
    }
}
