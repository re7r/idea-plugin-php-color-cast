package com.github.re7r.phpColorCast.settings.table.columns.delete

import java.awt.Component
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

class SettingsDeleteButtonRenderer(
) : TableCellRenderer, DefaultTableCellRenderer() {

    private val buttonRenderer: JButton = JButton()

    init {
        buttonRenderer.isOpaque = true
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {

        buttonRenderer.text = value?.toString() ?: "Delete"

        if (isSelected) {
            buttonRenderer.background = table.selectionBackground
            buttonRenderer.foreground = table.selectionForeground
        } else {
            buttonRenderer.background = UIManager.getColor("Button.background")
        }

        val originalFont = buttonRenderer.font
        buttonRenderer.font = originalFont.deriveFont(
            maxOf(originalFont.size - 3, 11).toFloat()
        )

        return buttonRenderer
    }
}