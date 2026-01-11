package com.github.re7r.phpColorCast.annotators

import com.github.re7r.phpColorCast.state.StateItem
import com.intellij.openapi.components.Service
import java.util.concurrent.ConcurrentHashMap
import com.intellij.openapi.editor.colors.TextAttributesKey

@Service(Service.Level.PROJECT)
class TypeColorCache {
    var isInitialized = false
    var cachedArrayType: StateItem? = null
    val colorCache = ConcurrentHashMap<String, TextAttributesKey>()

    fun clear() {
        isInitialized = false
        cachedArrayType = null
        colorCache.clear()
    }
}