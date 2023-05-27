# Mock StatsD


StatsD is a simple daemon for easy stats aggregation which is commonly used for monitoring applications. The basic idea is to send different types of metrics (like counters, timers, gauges) from your application to StatsD, which then periodically aggregates the metrics and pushes them to Graphite (or some other defined backend).
