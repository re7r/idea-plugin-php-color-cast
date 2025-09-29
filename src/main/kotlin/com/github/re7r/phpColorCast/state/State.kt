package com.github.re7r.phpColorCast.state

import com.intellij.util.xmlb.annotations.XCollection

data class State(
    @XCollection
    var types: MutableList<StateItem> = mutableListOf(),

    var properties: Boolean = true,
    var references: Boolean = true,
    var variables: Boolean = true
)