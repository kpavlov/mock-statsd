package me.kpavlov.mocks.statsd.server

internal enum class MetricType(private val code: String) {

    COUNTER("c"),
    TIMER("ms"),
    GAUGE("g"),
    HISTOGRAM("h"),
    SET("s"),
}

internal fun getMetricType(type: String): MetricType = when (type) {
    "c" -> MetricType.COUNTER
    "ms" -> MetricType.TIMER
    "g" -> MetricType.GAUGE
    "h" -> MetricType.HISTOGRAM
    "s" -> MetricType.SET
    else -> throw IllegalArgumentException("Unknown metric type: $type")
}
