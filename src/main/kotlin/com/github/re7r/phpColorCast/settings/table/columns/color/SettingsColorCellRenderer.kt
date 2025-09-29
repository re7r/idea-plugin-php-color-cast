package com.github.re7r.phpColorCast.settings.table.columns.color

import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.table.TableCellRenderer

class SettingsColorCellRenderer : JLabel(), TableCellRenderer {

    init {
        isOpaque = true
        horizontalAlignment = LEFT
        border = JBUI.Borders.empty(0, JBUIScale.scale(5))
    }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val hexColor = value.toString()
        val color: Color = try {
            Color.decode(hexColor)
        } catch (_: Exception) {
            JBColor.RED
        }

        text = hexColor
        icon = ColorIcon(JBUIScale.scale(12), color)

        background = if (isSelected) table.selectionBackground else UIManager.getColor("Table.background")
        foreground = UIManager.getColor("Table.foreground")

        val originalFont = this.font
        this.font = originalFont.deriveFont(
            maxOf(this.font.size - 3, 11).toFloat()
        )

        return this
    }
}
