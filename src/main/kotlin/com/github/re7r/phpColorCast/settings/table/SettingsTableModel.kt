package com.github.re7r.phpColorCast.settings.table

import com.github.re7r.phpColorCast.state.StateItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.swing.table.AbstractTableModel
import kotlin.random.Random

class SettingsTableModel : AbstractTableModel() {
    private val columnNames = arrayOf("Type", "Color", "")
    private var saved: List<StateItem> = listOf()
    private var data: MutableList<TypeEntry> = mutableListOf()

    data class TypeEntry(
        var type: StateItem,
        var state: EntryState = EntryState(),
    ) {
        fun validate(): Boolean {
            state.isValid = true
            state.errors.clear()

            if (!type.path.isBlank() && !isValidPhpType(type.path)) {
                state.isValid = false
                state.errors += "Invalid Type"
            }

            return state.isValid
        }

        private fun isValidPhpType(type: String): Boolean {
            // segment: starts with letter or _, then letters, numbers, _
            val segment = """[a-zA-Z_][a-zA-Z0-9_]*"""
            // full type: optional leading \, then segment(\segment)*
            return Regex("""^\\?$segment(?:\\$segment)*$""").matches(type)
        }
    }

    data class EntryState(
        var isModified: Boolean = false,
        var isValid: Boolean = true,
        var errors: MutableList<String> = mutableListOf(),
    )

    override fun getColumnCount() = 3
    override fun getRowCount() = data.size
    override fun getColumnName(column: Int) = columnNames[column]
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
            0 -> String::class.java
            1 -> String::class.java
            2 -> String::class.java
            else -> super.getColumnClass(columnIndex)
        }
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = data[rowIndex]
        return when (columnIndex) {
            0 -> entry.type.path
            1 -> entry.type.color
            2 -> "Delete"
            else -> throw IllegalArgumentException("Invalid column index: $columnIndex")
        }
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex > 1) return

        val newValue = value as? String ?: ""
        val entry = data[rowIndex]
        var changed = false

        when (columnIndex) {
            0 -> {
                if (entry.type.path != newValue) {
                    entry.type.path = newValue
                    changed = true
                }
            }

            1 -> {
                if (entry.type.color != newValue) {
                    entry.type.color = newValue
                    changed = true
                }
            }
        }

        if (changed) {
            entry.validate()
            entry.state.isModified = (rowIndex >= saved.size) || (saved.getOrNull(rowIndex) != entry.type)
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }

    fun entries(): ImmutableList<TypeEntry> {
        return data.toImmutableList()
    }

    fun list(): ImmutableList<StateItem> {
        return data
            .map { it.type }
            .filter { it.path.isNotBlank() }
            .toImmutableList()
    }

    fun load(list: List<StateItem>) {
        data = list.mapIndexed { index, stateType ->
            TypeEntry(
                stateType.copy(),
                EntryState(
                    isModified = (index >= saved.size) || (saved.getOrNull(index) != stateType)
                )
            )
        }.toMutableList()
        fireTableDataChanged()
    }

    fun sync() {
        this.saved = list().map { it.copy() }

        for (entry in data) {
            entry.state.isModified = false
        }
    }

    fun addRow() {
        data.add(TypeEntry(StateItem("", randomFunColorHex())))
        fireTableRowsInserted(data.size - 1, data.size - 1)
    }

    fun removeRow(rowIndex: Int) {
        if (rowIndex in data.indices) {
            data.removeAt(rowIndex)
            fireTableRowsDeleted(rowIndex, rowIndex)
        }
    }

    fun hasModifications(): Boolean {
        return list().map { it.path to it.color } != saved.map { it.path to it.color }
    }

    fun getEntry(rowIndex: Int): TypeEntry = data[rowIndex]

    companion object {
        private val funColors = listOf(
            "#FF77FF", // Pink Flamingo
            "#00BFFF", // Electric Blue
            "#FF9500", // Tangerine
            "#32CD32", // Lime Green
            "#9B30FF", // Purple Orchid
            "#FF3B30", // Strawberry Red
            "#3FFFD4", // Mint Aqua
            "#800080", // Purple
            "#FFD700", // Gold
            "#40E0D0", // Turquoise
            "#FF69B4", // Hot Pink
            "#8A2BE2", // Blue Violet
            "#FF4500", // Orange Red
            "#7FFF00", // Chartreuse
            "#00FA9A", // Medium Spring Green
            "#1E90FF", // Dodger Blue
            "#BA55D3", // Medium Orchid
            "#00CED1", // Dark Turquoise
            "#FF1493", // Deep Pink
            "#ADFF2F", // Green Yellow
            "#20B2AA", // Light Sea Green
            "#7B68EE", // Medium Slate Blue
            "#FFA500", // Orange
            "#66CDAA", // Medium Aquamarine
            "#DC143C", // Crimson
            "#00FF7F", // Spring Green
            "#4682B4", // Steel Blue
            "#DA70D6", // Orchid
            "#FF8C00", // Dark Orange
            "#9932CC"  // Dark Orchid
        )

        private fun randomFunColorHex(): String =
            funColors[Random.Default.nextInt(funColors.size)]
    }
}