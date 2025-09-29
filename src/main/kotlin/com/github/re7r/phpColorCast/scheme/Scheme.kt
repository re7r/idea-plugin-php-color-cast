package com.github.re7r.phpColorCast.scheme

import com.github.re7r.phpColorCast.state.State
import com.intellij.openapi.options.Scheme
import com.intellij.util.xmlb.annotations.Attribute

data class Scheme(
    @Attribute("name")
    private var scheme: String = "",
    @Transient
    var isProject: Boolean = false,
    var state: State = State()
) : Scheme {
    companion object {
        const val DEFAULT = "Default"
        const val PROJECT = "Project"
    }

    override fun getName(): String = scheme
}