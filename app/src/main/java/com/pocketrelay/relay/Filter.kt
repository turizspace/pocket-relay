package com.pocketrelay.relay

data class Filter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null
)

fun Filter.matches(e: NostrEvent): Boolean {
    ids?.let { if (e.id !in it) return false }
    authors?.let { if (e.pubkey !in it) return false }
    kinds?.let { if (e.kind !in it) return false }
    since?.let { if (e.created_at < it) return false }
    until?.let { if (e.created_at > it) return false }
    return true
}