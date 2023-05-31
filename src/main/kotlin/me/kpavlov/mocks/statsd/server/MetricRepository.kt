package me.kpavlov.mocks.statsd.server

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

internal class MetricRepository() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val metrics = ConcurrentHashMap<MetricId, Metric>()

    internal fun findMetric(metricName: String, tags: Map<String, String>? = null): Metric? {
        return metrics.entries
            .firstOrNull { it.key.matches(metricName, tags) }?.value
    }

    internal fun metricContents(
        metricName: String,
        tags: Map<String, String>? = null
    ): DoubleArray? {
        val metric = findMetric(metricName, tags)
        return if (metric is Metric.SetMetric) {
            metric.values()
        } else {
            null
        }
    }

    internal fun reset() {
        metrics.clear()
    }

    internal fun merge(
        metricType: String,
        metricName: String,
        tags: Map<String, String>,
        metricValue: Double,
        sampleRate: Double? = null
    ) {
        val metricId = MetricId(metricName, tags)
        if (logger.isTraceEnabled) {
            logger.trace("Tags: {}, ID={}", tags, metricId)
        }

        val metric = metrics.computeIfAbsent(metricId) { createMetric(metricType) }
        metric.merge(metricValue, sampleRate)
        if (logger.isDebugEnabled) {
            logger.debug("Updated value: {} = {}", metricId, metrics[metricId])
        }
    }
}
