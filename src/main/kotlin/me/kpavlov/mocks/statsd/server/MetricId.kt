package me.kpavlov.mocks.statsd.server

internal data class MetricId(
    val name: String,
    val tags: Map<String, String> = emptyMap()
) {
    fun matches(wantedName: String, wantedTags: Map<String, String>? = null): Boolean {
        if (name != wantedName) {
            return false
        }
        return if (wantedTags == null) {
            true
        } else if (wantedTags.size > tags.size) {
            false
        } else {
            val commonKeys = wantedTags.keys.intersect(tags.keys)
            commonKeys.count { tags[it] == wantedTags[it] } == commonKeys.size
        }
    }
}
