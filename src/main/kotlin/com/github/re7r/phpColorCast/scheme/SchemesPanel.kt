package com.github.re7r.phpColorCast.scheme

import com.github.re7r.phpColorCast.state.State
import com.intellij.application.options.schemes.AbstractSchemeActions
import com.intellij.application.options.schemes.SchemesModel
import com.intellij.application.options.schemes.SimpleSchemesPanel
import com.intellij.openapi.application.ApplicationManager

class SchemesPanel(
    private val manager: SchemesManager
) : SimpleSchemesPanel<Scheme>() {

    private var loaded = false
    private var schemeChangedEvent: ((scheme: Scheme) -> Unit)? = null
    private var stateUpdatedEvent: ((state: State) -> Unit)? = null

    init {
        isVisible = false
        ApplicationManager.getApplication().invokeLater {
            selectScheme(manager.getScheme())
            isVisible = true
            loaded = true
        }
    }

    fun onSchemeChange(callback: ((scheme: Scheme) -> Unit)?) {
        this.schemeChangedEvent = callback
    }

    fun onStateUpdate(callback: ((state: State) -> Unit)?) {
        this.stateUpdatedEvent = callback
    }

    fun isModified(): Boolean {
        return loaded && selectedScheme != manager.getScheme()
    }

    fun apply() {
        if (selectedScheme != null) manager.setScheme(selectedScheme)
    }

    fun reset() {
        resetSchemes(manager.getSchemes())
        selectScheme(manager.getScheme())
    }

    override fun createSchemeActions(): AbstractSchemeActions<Scheme> {
        return object : AbstractSchemeActions<Scheme>(this) {

            override fun copyToIDE(scheme: Scheme) {
                selectScheme(manager.getAppScheme())
                schemeChangedEvent?.invoke(selectedScheme)
                stateUpdatedEvent?.invoke(scheme.state)
            }

            override fun copyToProject(scheme: Scheme) {
                selectScheme(manager.getProjectScheme())
                schemeChangedEvent?.invoke(selectedScheme)
                stateUpdatedEvent?.invoke(scheme.state)
            }

            override fun resetScheme(scheme: Scheme) {
                stateUpdatedEvent?.invoke(State())
            }

            override fun onSchemeChanged(scheme: Scheme?) {
                if (isVisible) {
                    schemeChangedEvent?.invoke(scheme ?: manager.getScheme())
                }
            }

            override fun duplicateScheme(scheme: Scheme, newName: String) {
                throw UnsupportedOperationException("Duplicate scheme is not supported")
            }

            override fun renameScheme(scheme: Scheme, newName: String) {
                throw UnsupportedOperationException("Rename scheme is not supported")
            }

            override fun getSchemeType(): Class<Scheme> = Scheme::class.java
        }
    }

    override fun getModel(): SchemesModel<Scheme> {
        return object : SchemesModel<Scheme> {
            override fun canDeleteScheme(scheme: Scheme) = false
            override fun canDuplicateScheme(scheme: Scheme) = false
            override fun canRenameScheme(scheme: Scheme) = false
            override fun canResetScheme(scheme: Scheme) = true

            override fun isProjectScheme(scheme: Scheme): Boolean {
                return scheme.isProject
            }

            override fun containsScheme(name: String, isProjectScheme: Boolean): Boolean {
                return manager.getSchemes().any { it.name == name && it.isProject == isProjectScheme }
            }

            override fun differsFromDefault(scheme: Scheme): Boolean {
                return manager.differsFromDefault(scheme)
            }

            override fun removeScheme(scheme: Scheme) {
                throw UnsupportedOperationException("Remove scheme is not supported")
            }
        }
    }

    override fun supportsProjectSchemes() = true
    override fun highlightNonDefaultSchemes() = true
    override fun useBoldForNonRemovableSchemes() = true
}