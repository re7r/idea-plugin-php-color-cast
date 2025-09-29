package com.github.re7r.phpColorCast.scheme

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.xmlb.annotations.XCollection

abstract class SchemeStore(
    schemes: List<Scheme>,
    current: String? = null,
) : PersistentStateComponent<SchemeStore.StoreState> {

    data class StoreState(
        @XCollection
        var schemes: List<Scheme> = emptyList(),
        var current: String? = null,
    )

    private var state = StoreState(
        schemes, current
    )

    override fun getState(): StoreState = state

    override fun loadState(payload: StoreState) {
        state = payload
    }

    fun getSchemes(): List<Scheme> {
        return state.schemes
    }

    fun getSchemeByName(name: String): Scheme? {
        return state.schemes.find { it.name == name }
    }

    fun getCurrentScheme(): Scheme? {
        return if (state.current != null) getSchemeByName(state.current!!) else null
    }

    fun setCurrentScheme(scheme: Scheme?) {
        state.current = scheme?.name
    }

    fun differsFromDefault(scheme: Scheme): Boolean {
        return scheme.state.types.isNotEmpty()
    }
}
