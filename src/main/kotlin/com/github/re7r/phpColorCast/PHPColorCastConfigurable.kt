package com.github.re7r.phpColorCast

import com.github.re7r.phpColorCast.annotators.TypeColorAnnotator
import com.github.re7r.phpColorCast.scheme.SchemesManager
import com.github.re7r.phpColorCast.scheme.SchemesPanel
import com.github.re7r.phpColorCast.settings.SettingsPanel
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBBox
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class PHPColorCastConfigurable(private val project: Project) : SearchableConfigurable {

    private var schemes: SchemesPanel? = null
    private var settings: SettingsPanel? = null

    override fun getId() = "PHPColorCast"
    override fun getDisplayName() = "PHP Color Cast"

    override fun createComponent(): JComponent? {
        val manager = project.getService(SchemesManager::class.java)

        settings = SettingsPanel(manager).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }

        schemes = SchemesPanel(manager).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            setSeparatorVisible(false)

            onSchemeChange { scheme ->
                settings!!.reset(scheme)
            }

            onStateUpdate { state ->
                settings!!.refresh(state)
            }
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(schemes)
            add(JBBox.createVerticalStrut(12))
            add(settings)
        }
    }

    override fun isModified(): Boolean {
        return schemes!!.isModified() || settings!!.isModified()
    }

    override fun reset() {
        schemes!!.reset()
        settings!!.reset()
    }

    override fun apply() {
        schemes!!.apply()
        settings!!.apply()

        TypeColorAnnotator.SharedState.clear()
        reopenAllPhpFiles(project)
    }

    override fun disposeUIResources() {
        schemes = null
        settings = null
    }

    private fun reopenAllPhpFiles(project: Project) {
        val manager = FileEditorManager.getInstance(project)
        val openFiles = manager.openFiles

        for (file in openFiles) {
            if (!file.fileType.defaultExtension.equals("php", ignoreCase = true)) continue
            val editor = manager.selectedTextEditor ?: continue
            if (editor.document != FileDocumentManager.getInstance().getDocument(file)) continue

            val caretOffset = editor.caretModel.offset
            val scrollOffset = editor.scrollingModel.verticalScrollOffset

            manager.closeFile(file)
            val reopenedEditors = manager.openFile(file, true)
            val reopenedEditor = reopenedEditors.find { it is TextEditor } as? TextEditor ?: continue
            val newEditor = reopenedEditor.editor

            if (caretOffset <= newEditor.document.textLength) {
                newEditor.caretModel.moveToOffset(caretOffset)
            }

            newEditor.scrollingModel.scrollVertically(scrollOffset)
        }
    }
}