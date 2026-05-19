package com.clearpath.xray_compose.data.tempstore

import com.github.f4b6a3.uuid.UuidCreator
import java.util.concurrent.ConcurrentHashMap

object TempStore {
    private val store = ConcurrentHashMap<String, Any>()

    // Put a value and return a token (string). Caller should pass token in nav args.
    fun put(value: Any): String {
        val token = UuidCreator.getTimeOrderedEpoch().toString()
        store[token] = value
        return token
    }

    // Get and remove (one-time), null if missing.
    @Suppress("UNCHECKED_CAST")
    fun <T> consume(token: String?): T? {
        if (token == null) return null
        val v = store.remove(token)
        return v as? T
    }

    // Peek without removing
    @Suppress("UNCHECKED_CAST")
    fun <T> peek(token: String?): T? {
        if (token == null) return null
        return store[token] as? T
    }

    // Explicit remove
    fun remove(token: String?) {
        if (token == null) return
        store.remove(token)
    }

    // Optional: clear all (for debug/tests)
    fun clearAll() {
        store.clear()
    }
}