package com.github.yankee42.aconsosync.cli

import com.github.yankee42.aconsosync.gui.MainFrame
import io.ktor.util.error
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.util.concurrent.Callable
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

@CommandLine.Command(subcommands = [Cli::class])
class Main : Callable<Int> {
    override fun call(): Int {
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
        return 0
    }
}
