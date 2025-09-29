package com.github.re7r.phpColorCast.settings.table.columns.delete

import com.github.re7r.phpColorCast.settings.table.SettingsTableModel
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.TableCellEditor

class SettingsDeleteButtonEditor(val model: SettingsTableModel) : AbstractCellEditor(), TableCellEditor,
    ActionListener {

    private val button: JButton = JButton("Delete")
    private var row: Int = 0

    init {
        button.isFocusPainted = false
        button.addActionListener(this)
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
    ): Component {
        this.row = row
        button.text = "Delete"
        return button
    }

    override fun getCellEditorValue(): Any {
        return "Delete"
    }

    override fun actionPerformed(e: ActionEvent?) {
        SwingUtilities.invokeLater {
            fireEditingStopped()
            model.removeRow(row)
        }
    }
}
