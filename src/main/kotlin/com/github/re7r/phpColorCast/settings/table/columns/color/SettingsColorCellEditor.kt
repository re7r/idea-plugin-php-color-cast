package com.github.re7r.phpColorCast.settings.table.columns.color

import com.github.re7r.phpColorCast.settings.table.SettingsTableModel
import com.intellij.ui.ColorChooserService
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.ColorIcon
import java.awt.Color
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class SettingsColorCellEditor(val model: SettingsTableModel) : AbstractCellEditor(), TableCellEditor {
    private var editingRow = -1
    private var editingColumn = -1
    private var currentColorHex: String = DEFAULT_COLOR_HEX
    private val button = JButton()

    companion object {
        private const val DEFAULT_COLOR_HEX = "#FF0000"
    }

    init {
        button.addActionListener {
            val initialColor = try { Color.decode(currentColorHex) } catch (_: Exception) { Color.decode(DEFAULT_COLOR_HEX) }

            ColorChooserService.instance.showPopup(
                project = null,
                showAlpha = false,
                currentColor = initialColor,
                listener = { newColor, _ ->
                    if (newColor != null) {
                        currentColorHex = toHex(newColor)
                        model.setValueAt(currentColorHex, editingRow, editingColumn)
                        stopCellEditing()
                    } else {
                        cancelCellEditing()
                    }
                }
            )
        }
    }

    private fun toHex(color: Color): String =
        String.format("#%02X%02X%02X", color.red, color.green, color.blue)

    override fun getCellEditorValue(): Any = currentColorHex


    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        editingRow = row
        editingColumn = column
        currentColorHex = value?.toString() ?: DEFAULT_COLOR_HEX
        val color = try { Color.decode(currentColorHex) } catch (_: Exception) { Color.decode(DEFAULT_COLOR_HEX) }
        button.text = currentColorHex
        button.icon = ColorIcon(JBUIScale.scale(12), color)
        return button
    }
}
