package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.google.firebase.firestore.DocumentSnapshot

/**
 * A Firestore document as a plain map, so the mappers run in plain JUnit tests without Firebase.
 * Wrong types read as missing instead of throwing like [DocumentSnapshot.getString] does.
 */
internal typealias Fields = Map<String, Any?>

internal fun DocumentSnapshot.fields(): Fields = data.orEmpty()

internal fun Fields.string(key: String): String? = this[key] as? String
internal fun Fields.long(key: String): Long? = (this[key] as? Number)?.toLong()
internal fun Fields.int(key: String): Int? = (this[key] as? Number)?.toInt()
internal fun Fields.bool(key: String): Boolean? = this[key] as? Boolean
internal fun Fields.strings(key: String): List<String>? = (this[key] as? List<*>)?.filterIsInstance<String>()

/** A nested map such as checklist items or labels, with its entries as [Fields]. */
internal fun Fields.entries(key: String): List<Pair<String, Fields>> =
    (this[key] as? Map<*, *>)?.mapNotNull { (entryKey, value) ->
        val id = entryKey as? String ?: return@mapNotNull null
        @Suppress("UNCHECKED_CAST")
        val fields = value as? Map<String, Any?> ?: return@mapNotNull null
        id to fields
    }.orEmpty()
