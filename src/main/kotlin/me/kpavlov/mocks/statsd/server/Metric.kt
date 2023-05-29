package me.kpavlov.mocks.statsd.server

import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * https://github.com/statsd/statsd/blob/master/docs/metric_types.md
 */
internal interface Metric {

    fun merge(other: Number, sampleRate: Double?)
    fun value(): Number

    data class Counter(val value: AtomicLong = AtomicLong(0L)) : Metric {
        override fun merge(other: Number, sampleRate: Double?) {
            value.addAndGet(other.toLong())
        }

        override fun value() = value
    }

    data class Timer(val value: AtomicLong = AtomicLong(0L)) : Metric {
        override fun merge(other: Number, sampleRate: Double?) {
            value.addAndGet(other.toLong())
        }

        override fun value() = value
    }

    data class Gauge(val value: AtomicReference<Double> = AtomicReference<Double>(0.0)) : Metric {
        override fun merge(other: Number, sampleRate: Double?) {
            value.accumulateAndGet(other.toDouble(), Double::plus)
        }

        override fun value(): Double = value.get()
    }

    data class Histogram(val value: AtomicReference<Double> = AtomicReference<Double>(0.0)) :
        Metric {
        override fun merge(other: Number, sampleRate: Double?) {
            value.set(other.toDouble())
        }

        override fun value(): Double = value.get()
    }

    data class SetMetric(
        private val value: ConcurrentSkipListSet<Double> = ConcurrentSkipListSet(),
    ) : Metric {
        override fun merge(other: Number, sampleRate: Double?) {
            value.add(other.toDouble())
        }

        override fun value(): Double {
            return value.elementAtOrElse(0) { 0.0 }
        }

        fun values(): Array<Double> = value.toTypedArray()
    }
}

internal fun createMetric(type: String): Metric = when (type) {
    "c" -> Metric.Counter()
    "ms" -> Metric.Timer()
    "g" -> Metric.Gauge()
    "h" -> Metric.Histogram()
    "s" -> Metric.SetMetric()
    else -> throw IllegalArgumentException("Unknown metric type: $type")
}
