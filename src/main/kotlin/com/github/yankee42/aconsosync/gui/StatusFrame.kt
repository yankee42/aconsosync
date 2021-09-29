package com.github.yankee42.aconsosync.gui

import com.github.yankee42.aconsosync.AconsoEvent
import com.github.yankee42.aconsosync.Document
import com.github.yankee42.aconsosync.DocumentAlreadyExistsEvent
import com.github.yankee42.aconsosync.DocumentListDownloadedEvent
import com.github.yankee42.aconsosync.DocumentStartSyncEvent
import com.github.yankee42.aconsosync.DocumentSyncCompleteEvent
import com.github.yankee42.aconsosync.DocumentSyncEvent
import java.awt.GridLayout
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.AbstractTableModel

class StatusFrame(parent: JFrame) : JDialog(parent, "Aconso Sync - Status", true) {
    private val statusTable = StatusTable()

    fun processEvent(it: AconsoEvent) {
        statusTable.processAconsoEvent(it)
    }

    init {
        layout = GridLayout(2, 1)
        setSize(800, 500)
        add(JScrollPane(statusTable))
        add(JScrollPane(SwingLogger.textPane))
    }
}
