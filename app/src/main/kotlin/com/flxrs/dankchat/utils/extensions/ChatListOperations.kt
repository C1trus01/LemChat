package com.flxrs.dankchat.utils.extensions

import com.flxrs.dankchat.data.chat.ChatItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList

// Persistent lists share structure between the emitted snapshots instead of copying the
// whole buffer per message
private fun List<ChatItem>.asPersistent(): PersistentList<ChatItem> = this as? PersistentList<ChatItem> ?: toPersistentList()

fun List<ChatItem>.addAndLimit(
    item: ChatItem,
    scrollBackLength: Int,
    onMessageRemoved: (ChatItem) -> Unit,
): List<ChatItem> = asPersistent().mutate { list ->
    list.add(item)
    while (list.size > scrollBackLength) {
        onMessageRemoved(list.removeAt(0))
    }
}

fun List<ChatItem>.addAndLimit(
    items: Collection<ChatItem>,
    scrollBackLength: Int,
    onMessageRemoved: (ChatItem) -> Unit,
    checkForDuplications: Boolean = false,
): List<ChatItem> = when {
    checkForDuplications -> {
        // Single-pass dedup via LinkedHashMap, then sort and trim.
        // putIfAbsent keeps existing (live) messages over history duplicates.
        val deduped = LinkedHashMap<String, ChatItem>(size + items.size)
        for (item in this) {
            deduped[item.message.id] = item
        }
        for (item in items) {
            deduped.putIfAbsent(item.message.id, item)
        }
        val sorted = deduped.values.sortedBy { it.message.timestamp }
        val excess = (sorted.size - scrollBackLength).coerceAtLeast(0)
        for (i in 0 until excess) {
            onMessageRemoved(sorted[i])
        }
        when {
            excess > 0 -> sorted.subList(excess, sorted.size).toPersistentList()
            else -> sorted.toPersistentList()
        }
    }

    else -> {
        asPersistent().mutate { list ->
            list.addAll(items)
            while (list.size > scrollBackLength) {
                onMessageRemoved(list.removeAt(0))
            }
        }
    }
}

/** Adds an item and trims the list inline. For use inside `toMutableList().apply { }` blocks to avoid a second mutable copy. */
internal fun MutableList<ChatItem>.addAndTrimInline(
    item: ChatItem,
    scrollBackLength: Int,
    onMessageRemoved: (ChatItem) -> Unit,
) {
    add(item)
    while (size > scrollBackLength) {
        onMessageRemoved(removeAt(index = 0))
    }
}
