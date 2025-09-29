package com.github.re7r.phpColorCast.settings.table

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.table.TableCellRenderer

class SettingsCellRenderer : JLabel(), TableCellRenderer {

    private val defaultColor = UIManager.getColor("Table.foreground")
    private val modifiedColor = JBColor(Color(70, 127, 249), Color(107, 155, 250))
    private val invalidColor = JBColor.RED

    init {
        isOpaque = true
        horizontalAlignment = LEFT
    }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val model = table.model as SettingsTableModel
        val entry = model.getEntry(row)

        border = JBUI.Borders.empty(0, 10)

        background = when (isSelected) {
            true -> table.selectionBackground
            false -> UIManager.getColor("Table.background")
        }

        foreground = when {
            !entry.state.isValid -> invalidColor
            entry.state.isModified -> modifiedColor
            else -> defaultColor
        }

        val messages = entry.state.errors.toMutableList()

        if (messages.isEmpty()) {
            when {
                entry.state.isModified -> messages += "Unsaved entry"
            }
        }

        text = value?.toString() ?: ""
        toolTipText = messages.joinToString(". ")

        return this
    }
}