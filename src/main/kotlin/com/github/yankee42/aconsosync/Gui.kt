package com.github.yankee42.aconsosync

import com.github.yankee42.aconsosync.gui.MainFrame
import io.ktor.util.*
import org.slf4j.LoggerFactory
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        LoggerFactory.getLogger("defaultExceptionHandler").error(throwable)
        JOptionPane.showMessageDialog(
            null,
            "An error occured:\n\n${throwable.stackTraceToString()}",
            "error",
            JOptionPane.ERROR_MESSAGE
        )
    }
    SwingUtilities.invokeLater { MainFrame() }
}
