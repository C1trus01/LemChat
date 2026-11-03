package com.flxrs.dankchat.ui.chat

import androidx.collection.LruCache

/**
 * Assigns stable ordinals to messages by ID, so that a message's even/odd
 * status never changes when other messages are added or evicted from the list.
 *
 * Each display context (ViewModel) should use its own tracker instance.
 */
class CheckeredMessageTracker {
    private val ordinals = LruCache<String, Long>(ORDINAL_CACHE_SIZE)
    private var nextOrdinal = 0L

    fun isAlternate(id: String): Boolean {
        val ordinal = ordinals.get(id) ?: (nextOrdinal++).also { ordinals.put(id, it) }
        return ordinal % 2 == 0L
    }

    companion object {
        // Above the maximum scrollback, ids evicted here are long gone from the message list
        private const val ORDINAL_CACHE_SIZE = 2048
    }
}
