package com.github.re7r.phpColorCast.scheme

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SchemesManager(project: Project) {

    private val proj = project.getService(ProjectSchemeStore::class.java)
    private val app = ApplicationManager.getApplication().getService(AppSchemeStore::class.java)

    @Service(Service.Level.APP)
    @State(name = "phpColorCast", storages = [Storage("php-color-cast.xml")])
    private class AppSchemeStore : SchemeStore(
        schemes = listOf(Scheme(Scheme.DEFAULT, isProject = false)),
        current = Scheme.DEFAULT,
    )

    @Service(Service.Level.PROJECT)
    @State(name = "phpColorCast", storages = [Storage("php-color-cast.xml")])
    private class ProjectSchemeStore : SchemeStore(
        schemes = listOf(Scheme(Scheme.PROJECT, isProject = true)),
        current = Scheme.PROJECT,
    )

    fun getSchemes(): List<Scheme> {
        val schemes = app.getSchemes().toMutableList()
        schemes.addAll(proj.getSchemes())
        return schemes
    }

    fun getScheme(): Scheme {
        return proj.getCurrentScheme() ?: app.getCurrentScheme()!!
    }

    fun setScheme(scheme: Scheme) {
        if (scheme.isProject) {
            proj.setCurrentScheme(scheme)
        } else {
            proj.setCurrentScheme(null)
            app.setCurrentScheme(scheme)
        }
    }

    fun getAppScheme(): Scheme {
        return app.getSchemeByName(Scheme.DEFAULT)!!
    }

    fun getProjectScheme(): Scheme {
        return proj.getSchemeByName(Scheme.PROJECT)!!
    }

    fun differsFromDefault(scheme: Scheme): Boolean {
        return if (scheme.isProject) proj.differsFromDefault(scheme)
        else app.differsFromDefault(scheme)
    }
}