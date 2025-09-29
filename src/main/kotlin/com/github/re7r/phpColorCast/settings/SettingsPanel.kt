package com.github.re7r.phpColorCast.settings

import com.github.re7r.phpColorCast.scheme.Scheme
import com.github.re7r.phpColorCast.scheme.SchemesManager
import com.github.re7r.phpColorCast.settings.table.SettingsCellRenderer
import com.github.re7r.phpColorCast.settings.table.SettingsTableModel
import com.github.re7r.phpColorCast.settings.table.columns.color.SettingsColorCellEditor
import com.github.re7r.phpColorCast.settings.table.columns.color.SettingsColorCellRenderer
import com.github.re7r.phpColorCast.settings.table.columns.delete.SettingsDeleteButtonEditor
import com.github.re7r.phpColorCast.settings.table.columns.delete.SettingsDeleteButtonRenderer
import com.github.re7r.phpColorCast.state.State
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*

class SettingsPanel(private val manager: SchemesManager) : JPanel(BorderLayout()) {

    private val model = SettingsTableModel()
    private val table = JBTable(model)

    private val propertiesCheckBox = JCheckBox("Properties")
    private val referencesCheckBox = JCheckBox("References")
    private val variablesCheckBox = JCheckBox("Variables")

    init {
        table.setDefaultRenderer(Any::class.java, SettingsCellRenderer())
        table.putClientProperty("terminateEditOnFocusLost", true)
        (table.getDefaultEditor(String::class.java) as? DefaultCellEditor)?.clickCountToStart = 1

        table.columnModel.getColumn(1).cellRenderer = SettingsColorCellRenderer()
        table.columnModel.getColumn(1).cellEditor = SettingsColorCellEditor(model)
        table.columnModel.getColumn(1).preferredWidth = 100
        table.columnModel.getColumn(1).maxWidth = 150
        table.columnModel.getColumn(1).minWidth = 80

        table.columnModel.getColumn(2).cellRenderer = SettingsDeleteButtonRenderer()
        table.columnModel.getColumn(2).cellEditor = SettingsDeleteButtonEditor(model)
        table.columnModel.getColumn(2).preferredWidth = 80
        table.columnModel.getColumn(2).maxWidth = 80
        table.columnModel.getColumn(2).minWidth = 80

        val top = JPanel().apply {
            border = JBUI.Borders.emptyBottom(12)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)

                add(propertiesCheckBox)
                add(Box.createHorizontalStrut(10))
                add(referencesCheckBox)
                add(Box.createHorizontalStrut(10))
                add(variablesCheckBox)
                add(Box.createHorizontalGlue())

                add(JButton("Add").apply {
                    addActionListener {
                        if (table.isEditing) table.cellEditor.stopCellEditing()
                        this@SettingsPanel.model.addRow()
                        table.editCellAt(this@SettingsPanel.model.rowCount - 1, 0)
                        table.editorComponent?.requestFocusInWindow()
                    }
                })
            })
        }

        add(top, BorderLayout.NORTH)
        add(JScrollPane(table), BorderLayout.CENTER)
    }

    fun isModified(): Boolean {
        return model.hasModifications() ||
                propertiesCheckBox.isSelected != manager.getScheme().state.properties ||
                referencesCheckBox.isSelected != manager.getScheme().state.references ||
                variablesCheckBox.isSelected != manager.getScheme().state.variables
    }

    fun apply() {
        for (entry in model.entries()) {
            if (!entry.state.isValid) {
                JOptionPane.showMessageDialog(
                    this,
                    "Invalid value found: '${entry.type.path}'. Please correct it.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }
        }

        manager.getScheme().let { scheme ->
            scheme.state.types.clear()
            scheme.state.types.addAll(model.list().distinctBy { it.path })
            scheme.state.properties = propertiesCheckBox.isSelected
            scheme.state.references = referencesCheckBox.isSelected
            scheme.state.variables = variablesCheckBox.isSelected
        }

        reset()
    }

    fun reset(scheme: Scheme? = null) {
        val state = scheme?.state ?: manager.getScheme().state
        propertiesCheckBox.isSelected = state.properties
        referencesCheckBox.isSelected = state.references
        variablesCheckBox.isSelected = state.variables
        refresh(state)
        model.sync()
    }

    fun refresh(state: State) {
        if (table.isEditing) table.cellEditor.stopCellEditing()
        model.load(state.types)
    }
}
