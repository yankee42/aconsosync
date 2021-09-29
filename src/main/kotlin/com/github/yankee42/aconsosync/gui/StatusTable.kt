package com.github.yankee42.aconsosync.gui

import com.github.yankee42.aconsosync.AconsoEvent
import com.github.yankee42.aconsosync.Document
import com.github.yankee42.aconsosync.DocumentAlreadyExistsEvent
import com.github.yankee42.aconsosync.DocumentListDownloadedEvent
import com.github.yankee42.aconsosync.DocumentStartSyncEvent
import com.github.yankee42.aconsosync.DocumentSyncCompleteEvent
import com.github.yankee42.aconsosync.DocumentSyncEvent
import java.awt.Color
import java.awt.Component
import java.lang.RuntimeException
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer

class StatusTable private constructor(private val tableModel: TableModel) : JTable(tableModel) {
    constructor() : this(TableModel())

    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component =
         super.prepareRenderer(renderer, row, column).also {
             it.background = tableModel[row].status.color
         }

    fun processAconsoEvent(it: AconsoEvent) {
        tableModel.processEvent(it)
    }
}

enum class TableColumn(val header: String, val getter: (document: TableRow) -> Any) {
    NAME("name", { it.document.fileName }),
    DATE("date", { it.document.documentDate}),
    STATUS("status", { it.status.label }),
}

enum class Status(val label: String, val color: Color) {
    PENDING("pending", Color(255, 150, 0)),
    DOWNLOADING("downloading", Color(170, 170, 255)),
    SYNC_COMPLETE("sync complete", Color(170, 255, 170)),
    NOTHING_TODO("nothing to do", Color(220, 220, 220)),
}

class TableRow(val document: Document, var status: Status)

class TableModel : AbstractTableModel() {
    private var documents: List<TableRow> = emptyList()

    operator fun get(row: Int) = documents[row]

    override fun getRowCount(): Int = documents.size
    override fun getColumnCount(): Int = TableColumn.values().size
    override fun getValueAt(row: Int, col: Int): Any = TableColumn.values()[col].getter(documents[row])
    override fun getColumnName(col: Int): String = TableColumn.values()[col].header

    fun processEvent(event: AconsoEvent) {
        when(event) {
            is DocumentListDownloadedEvent -> {
                documents = event.documentList.documents.map { TableRow(it, Status.PENDING) }
                SwingUtilities.invokeLater { fireTableDataChanged() }
            }
            is DocumentSyncEvent -> {
                val row = documents.indexOfFirst { it.document == event.document }
                documents[row].status = when(event) {
                    is DocumentAlreadyExistsEvent -> Status.NOTHING_TODO
                    is DocumentStartSyncEvent -> Status.DOWNLOADING
                    is DocumentSyncCompleteEvent -> Status.SYNC_COMPLETE
                }
                SwingUtilities.invokeLater { fireTableRowsUpdated(row, row) }
            }
        }
    }
}
